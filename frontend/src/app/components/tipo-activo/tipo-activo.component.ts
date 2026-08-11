import { CommonModule } from '@angular/common';
import { Component, inject, OnInit } from '@angular/core';
import { FormBuilder, FormControl, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { AuthService } from '../../services/auth.service';
import { TipoActivoService } from '../../services/tipo-activo.service';
import { TipoActivo } from '../../model/tipoActivo';
import { Empresa } from '../../model/empresa';
import { EmpresaService } from '../../services/empresa.service';
import { FormUtils } from '../../shared/form-utils';
import Swal from 'sweetalert2';

@Component({
  selector: 'app-tipo-activo',
  standalone: true,
  imports: [FormsModule, ReactiveFormsModule, CommonModule, MatAutocompleteModule],
  templateUrl: './tipo-activo.component.html',
  styleUrl: './tipo-activo.component.css'
})
export class TipoActivoComponent implements OnInit {

  authService = inject(AuthService);
  tipoActivoService = inject(TipoActivoService);
  empresaService = inject(EmpresaService);
  fb = inject(FormBuilder);

  tipoActivoForm!: FormGroup;

  // 🔥 Autocompletado "escribir para buscar" (mismo patrón que Activo),
  // manteniendo el envío por ID hacia el backend (empresaId).
  empresaControl = new FormControl();

  tipoActivos: TipoActivo[] = [];
  empresas: Empresa[] = [];
  empresasFiltradas: Empresa[] = [];

  esSuperAdmin = false;
  esAdminEmpresa = false;
  editando: boolean = false;
  mostrarNuevo = false;
  tipoActivoEditandoId: number | null = null;
  tipoActivoSeleccionado: any = null;

  page = 0;
  size = 10;

  totalPages = 0;
  totalElements = 0;

  ngOnInit() {
    this.esSuperAdmin = this.authService.isAdmin();
    this.esAdminEmpresa = this.authService.isAdminEmpresa();
    this.initForm();
    this.initAutocompletes();
    this.cargarTipoActivos();
    this.cargarEmpresas();
  }

  initForm() {
    this.tipoActivoForm = this.fb.group({
      id: [''],
      nombre: ['', Validators.required],
      descripcion: ['', Validators.required],
      vidaUtilReferencialMeses: ['', [Validators.required, Validators.pattern('^[0-9]+$')]],
      empresaId: [null, Validators.required],
      empresa: [''],
      activo: [true] // 👈 checkbox
    });
  }

  // 🔥 Filtra la lista de empresas a medida que se escribe y mantiene
  // sincronizado el empresaId real que se manda al backend.
  initAutocompletes() {
    this.empresaControl.valueChanges.subscribe(value => {
      const seleccionada = value && typeof value === 'object' ? value : null;
      this.tipoActivoForm.patchValue({ empresaId: seleccionada?.id || null });

      const search = (typeof value === 'string' ? value : value?.nombre || '').toLowerCase().trim();
      this.empresasFiltradas = !search
        ? this.empresas
        : this.empresas.filter(e => e.nombre.toLowerCase().includes(search));
    });
  }

  displayEmpresa = (empresa: any): string => empresa?.nombre ?? '';

  onFocusEmpresa() {
    this.empresasFiltradas = this.empresas;
  }

  // 🔥 Espera a que el combo de empresas ya haya cargado antes de setear
  // el valor mostrado en el autocompletado, para que muestre el nombre.
  setEmpresaSeleccionada(empresaId: number) {
    if (!this.empresas || this.empresas.length === 0) {
      setTimeout(() => this.setEmpresaSeleccionada(empresaId), 200);
      return;
    }
    const empresa = this.empresas.find(e => e.id === empresaId);
    if (empresa) {
      this.empresasFiltradas = [...this.empresas];
      setTimeout(() => this.empresaControl.setValue(empresa));
    }
  }

  cargarTipoActivos() {
    this.tipoActivoService.getAll(this.page, this.size).subscribe({
      next: (data) => {
        this.tipoActivos = data.content;
        this.totalPages = data.page.totalPages;
        this.totalElements = data.page.totalElements;
      },
      error: (err) => {
        Swal.fire({
          icon: 'error',
          title: 'Error',
          text: err.error?.error || 'Error desconocido'
        });
      }
    });
  }

  cargarEmpresas() {
    this.empresaService.getAll().subscribe(data => {
      this.empresas = data;
      this.empresasFiltradas = data;
    });
  }

  cambiarPagina(p: number) {
    this.page = p;
    this.cargarTipoActivos();
  }

  guardar() {
    if (!FormUtils.esValido(this.tipoActivoForm)) {
      const campo = FormUtils.getPrimerCampoInvalido(this.tipoActivoForm);
      FormUtils.marcarComoTocados(this.tipoActivoForm);
      Swal.fire({
        icon: 'warning',
        title: 'Formulario incompleto',
        text: `Revisa el campo: ${campo}`
      });

      console.log(FormUtils.getErrores(this.tipoActivoForm));

      return;
    }

    const tipoActivo: TipoActivo = this.tipoActivoForm.value;

    if (this.editando && this.tipoActivoEditandoId !== null) {
      // EDITAR
      Swal.fire({
        title: '¿Estás seguro?',
        text: 'Esta acción actualizará el tipo activo',
        icon: 'warning',
        showCancelButton: true,
        confirmButtonText: 'Sí, actualizar',
        cancelButtonText: 'Cancelar'
      }).then(result => {
        if (result.isConfirmed) {

          Swal.fire({
            title: 'Actualizando...',
            allowOutsideClick: false,
            didOpen: () => Swal.showLoading()
          });

          this.tipoActivoService.update(this.tipoActivoEditandoId!, tipoActivo).subscribe({
            next: () => {
              Swal.fire({
                icon: 'success',
                title: 'Actualizado',
                text: 'El tipo activo fue actualizado correctamente',
                timer: 2000,
                showConfirmButton: false
              });

              this.cargarTipoActivos(); // 🔄 refrescar tabla
              this.nuevo();
            },
            error: (err) => {
              console.log(err.error); // 👈 DEBUG
              Swal.fire({
                icon: 'error',
                title: 'Error',
                text: err.error?.message || 'No se pudo actualizar'
              });
            }
          });
        }
      });
    } else {
      // CREAR
      this.tipoActivoService.create(tipoActivo).subscribe({
        next: () => {
          this.resetForm();
          this.cargarTipoActivos();

          Swal.fire({
            icon: 'success',
            title: '¡Guardado!',
            text: 'El tipo activo fue creado correctamente',
            confirmButtonColor: '#3498db'
          });
        },
        error: () => {
          Swal.fire({
            icon: 'error',
            title: 'Error',
            text: 'No se pudo guardar el tipo activo'
          });
        }
      });
    }
  }
  
    resetForm() {
      this.tipoActivoForm.reset();
      this.empresaControl.reset();
      this.editando = false;
      this.tipoActivoEditandoId = null;
    }
  
    nuevo(){
      this.resetForm();
      this.mostrarNuevo = false;
    }
  
    editar(tipoActivo: TipoActivo) {
      this.editando = true;
      this.esSuperAdmin = true;
      this.tipoActivoEditandoId = tipoActivo.id!;
      this.tipoActivoSeleccionado = tipoActivo!;

      this.tipoActivoForm.patchValue({
        id: tipoActivo.id,
        nombre: tipoActivo.nombre,
        descripcion: tipoActivo.descripcion,
        vidaUtilReferencialMeses: tipoActivo.vidaUtilReferencialMeses,
        empresa: tipoActivo.empresa.nombre,
        empresaId: tipoActivo.empresa.id,
        activo: tipoActivo.activo
      });

      this.setEmpresaSeleccionada(tipoActivo.empresa.id!);

      if (this.authService.isAdmin() || this.authService.isAdminEmpresa()){
        this.mostrarNuevo = true;
      }

    }
  
  eliminar(id: number) {
    Swal.fire({
      title: '¿Estás seguro?',
      text: 'Esta acción dejará inactivo el tipo de activo',
      icon: 'warning',
      showCancelButton: true,
      confirmButtonText: 'Sí, eliminar',
      cancelButtonText: 'Cancelar'
    }).then(result => {
      if (result.isConfirmed) {

        // ✅ Llamar backend
        this.tipoActivoService.delete(id)
          .subscribe({
            next: () => {
              Swal.fire({
                icon: 'success',
                title: 'Eliminar',
                text: 'El tipo de activo se eliminó correctamente',
                timer: 2000,
                showConfirmButton: false
              });
              this.cargarTipoActivos(); // 🔄 refrescar tabla
              this.nuevo();
            },
            error: () => {    
              Swal.fire({
                icon: 'error',
                title: 'Error',
                text: 'No se pudo dar de baja al activo'
              });
            }
          });       
      }
    });
  }

}
