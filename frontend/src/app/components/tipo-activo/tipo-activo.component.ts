import { CommonModule } from '@angular/common';
import { Component, inject, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
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
  imports: [FormsModule, ReactiveFormsModule, CommonModule],
  templateUrl: './tipo-activo.component.html',
  styleUrl: './tipo-activo.component.css'
})
export class TipoActivoComponent implements OnInit {

  authService = inject(AuthService);
  tipoActivoService = inject(TipoActivoService);
  empresaService = inject(EmpresaService);
  fb = inject(FormBuilder);

  tipoActivoForm!: FormGroup;

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
      activo: [false] // 👈 checkbox
    });
  }

  cargarTipoActivos() {
    this.tipoActivoService.getAll(this.page, this.size).subscribe({
      next: (data) => {
        this.tipoActivos = data.content;
        this.totalPages = data.totalPages;
        this.totalElements = data.totalElements;
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

      if (this.authService.isAdmin() || this.authService.isAdminEmpresa()){
        this.mostrarNuevo = true;
      }  
  
    }
  
    confirmarEliminar(id: number) {
      //this.eliminar(id);
    }

}
