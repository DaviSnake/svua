import { CommonModule } from '@angular/common';
import { Component, inject, OnInit } from '@angular/core';
import { FormBuilder, FormControl, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { debounceTime, distinctUntilChanged } from 'rxjs';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { AuthService } from '../../services/auth.service';
import { BodegaService } from '../../services/bodega.service';
import { Bodega } from '../../model/bodega';
import { EmpresaService } from '../../services/empresa.service';
import { Empresa } from '../../model/empresa';
import { FormUtils } from '../../shared/form-utils';
import { calcularPaginasVisibles } from '../../shared/pagination.util';
import Swal from 'sweetalert2';

@Component({
  selector: 'app-bodega',
  standalone: true,
  imports: [FormsModule, ReactiveFormsModule, CommonModule, MatAutocompleteModule],
  templateUrl: './bodega.component.html',
  styleUrl: './bodega.component.css'
})
export class BodegaComponent implements OnInit {

  authService = inject(AuthService);
  bodegaService = inject(BodegaService);
  empresaService = inject(EmpresaService);
  fb = inject(FormBuilder);

  bodegaForm!: FormGroup;

  // 🔥 Autocompletado "escribir para buscar" (mismo patrón que Activo),
  // manteniendo el envío por ID hacia el backend (empresaId).
  empresaControl = new FormControl();

  bodegas: Bodega[] = [];
  empresas: Empresa[] = [];
  empresasFiltradas: Empresa[] = [];

  // 🔥 Filtro de la grilla por empresa (mismo patrón de autocompletado;
  // solo tiene efecto real para SUPER_ADMIN, que es el único rol que ve
  // registros de más de una empresa a la vez).
  filtroEmpresaControl = new FormControl();
  empresasFiltroFiltradas: Empresa[] = [];
  filtroEmpresaId: number | null = null;

  // 🔍 Busqueda de la grilla (todos los roles).
  busquedaControl = new FormControl('');
  busqueda: string = '';

  esSuperAdmin = false;
  esAdminEmpresa = false;
  editando: boolean = false;
  mostrarNuevo = false;
  bodegaEditandoId: number | null = null;
  bodegaSeleccionado: any = null;

  page = 0;
  size = 10;

  totalPages = 0;
  totalElements = 0;

  ngOnInit() {
    this.esSuperAdmin = this.authService.isAdmin();
    this.esAdminEmpresa = this.authService.isAdminEmpresa();
    this.initForm();
    this.initAutocompletes();
    this.cargarBodegas();
    this.cargarEmpresas();
  }

  initForm() {
    this.bodegaForm = this.fb.group({
      id: [''],
      nombre: ['', Validators.required],
      ubicacionFisica: ['', Validators.required],
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
      this.bodegaForm.patchValue({ empresaId: seleccionada?.id || null });

      const search = (typeof value === 'string' ? value : value?.nombre || '').toLowerCase().trim();
      this.empresasFiltradas = !search
        ? this.empresas
        : this.empresas.filter(e => e.nombre.toLowerCase().includes(search));
    });

    // 🔥 Filtro de la grilla por empresa. Solo recarga la grilla cuando
    // se selecciona una empresa (objeto) o cuando se borra el texto por
    // completo (para volver a ver todas); mientras se escribe, solo
    // filtra las opciones del desplegable.
    this.filtroEmpresaControl.valueChanges.subscribe(value => {
      const esObjeto = value && typeof value === 'object';
      const search = (esObjeto ? value.nombre : value || '').toLowerCase().trim();

      this.empresasFiltroFiltradas = !search
        ? this.empresas
        : this.empresas.filter(e => e.nombre.toLowerCase().includes(search));

      if (esObjeto) {
        this.filtroEmpresaId = value.id;
        this.page = 0;
        this.cargarBodegas();
      } else if (!search && this.filtroEmpresaId !== null) {
        this.filtroEmpresaId = null;
        this.page = 0;
        this.cargarBodegas();
      }
    });

    // 🔍 Busqueda de la grilla por nombre: espera 400ms
    // sin escribir antes de consultar al backend (evita una request por
    // tecla).
    this.busquedaControl.valueChanges
      .pipe(debounceTime(400), distinctUntilChanged())
      .subscribe(value => {
        this.busqueda = (value || '').trim();
        this.page = 0;
        this.cargarBodegas();
      });
  }

  displayEmpresa = (empresa: any): string => empresa?.nombre ?? '';

  onFocusEmpresa() {
    this.empresasFiltradas = this.empresas;
  }

  onFocusFiltroEmpresa() {
    this.empresasFiltroFiltradas = this.empresas;
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

  cargarBodegas() {
    this.bodegaService.getAll(this.page, this.size, this.filtroEmpresaId, this.busqueda).subscribe({
      next: (data) => {
        this.bodegas = data.content;
        this.totalPages = data.page.totalPages;
        this.totalElements = data.page.totalElements;
      },
      error: () => {
        console.log("error");
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
    this.cargarBodegas();
  }

  // 🔥 Botones de página a mostrar (con "..." si hay muchas), en vez de
  // listar un botón por cada página.
  paginasVisibles(): number[] {
    return calcularPaginasVisibles(this.page, this.totalPages);
  }

  guardar() {
    if (!FormUtils.esValido(this.bodegaForm)) {
      const campo = FormUtils.getPrimerCampoInvalido(this.bodegaForm);
      FormUtils.marcarComoTocados(this.bodegaForm);
      Swal.fire({
        icon: 'warning',
        title: 'Formulario incompleto',
        text: `Revisa el campo: ${campo}`
      });

      console.log(FormUtils.getErrores(this.bodegaForm));

      return;
    }

    const bodega: Bodega = this.bodegaForm.value;

    if (this.editando && this.bodegaEditandoId !== null) {
      // EDITAR
      Swal.fire({
        title: '¿Estás seguro?',
        text: 'Esta acción actualizará la bodega',
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

          this.bodegaService.update(this.bodegaEditandoId!, bodega).subscribe({
            next: () => {
              Swal.fire({
                icon: 'success',
                title: 'Actualizado',
                text: 'La bodega fue actualizado correctamente',
                timer: 2000,
                showConfirmButton: false
              });

              this.cargarBodegas(); // 🔄 refrescar tabla
            },
            error: (err) => {
              console.log(err.error); // 👈 DEBUG
              Swal.fire({
                icon: 'error',
                title: 'Error',
                text: err.error?.error || 'No se pudo actualizar'
              });
            }
          });
        }
      });
    } else {
      // CREAR
      this.bodegaService.create(bodega).subscribe({
        next: () => {
          this.nuevo();
          this.cargarBodegas();

          Swal.fire({
            icon: 'success',
            title: '¡Guardado!',
            text: 'La bodega fue creado correctamente',
            confirmButtonColor: '#3498db'
          });
        },
        error: () => {
          Swal.fire({
            icon: 'error',
            title: 'Error',
            text: 'No se pudo guardar la bodega'
          });
        }
      });
    }
  }
  
  resetForm() {
    this.bodegaForm.reset();
    this.empresaControl.reset();
    this.editando = false;
    this.bodegaEditandoId = null;
  }

  nuevo(){
    this.resetForm();
    this.mostrarNuevo = false;
  }

  editar(bodega: Bodega) {
    this.editando = true;
    this.esSuperAdmin = this.authService.isAdmin();
    this.bodegaEditandoId = bodega.id!;
    this.bodegaSeleccionado = bodega!;

    this.bodegaForm.patchValue({
      id: bodega.id,
      nombre: bodega.nombre,
      ubicacionFisica: bodega.ubicacionFisica,
      empresaId: bodega.empresa.id,
      activo: bodega.activo
    });

    this.setEmpresaSeleccionada(bodega.empresa.id!);

    if (this.authService.isAdmin() || this.authService.isAdminEmpresa()){
      this.mostrarNuevo = true;
    }
  }

  eliminar(id: number) {
    Swal.fire({
      title: '¿Estás seguro?',
      text: 'Esta acción dejará inactivo la bodega',
      icon: 'warning',
      showCancelButton: true,
      confirmButtonText: 'Sí, eliminar',
      cancelButtonText: 'Cancelar'
    }).then(result => {
      if (result.isConfirmed) {

        // ✅ Llamar backend
        this.bodegaService.delete(id)
          .subscribe({
            next: () => {
              Swal.fire({
                icon: 'success',
                title: 'Eliminar',
                text: 'La bodega se eliminó correctamente',
                timer: 2000,
                showConfirmButton: false
              });
              this.cargarBodegas(); // 🔄 refrescar tabla
              this.nuevo();
            },
            error: () => {    
              Swal.fire({
                icon: 'error',
                title: 'Error',
                text: 'No se pudo eliminar la bodega'
              });
            }
          });
      }
    });
  }

  // 🔥 trackBy para la tabla principal de bodegas.
  trackByBodegaId(index: number, bodega: any): any {
    return bodega?.id ?? index;
  }

}
