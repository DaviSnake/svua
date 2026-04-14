import { CommonModule } from '@angular/common';
import { Component, inject, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { UbicacionService } from '../../services/ubicacion.service';
import { AuthService } from '../../services/auth.service';
import { Ubicacion } from '../../model/ubicacion';
import { Empresa } from '../../model/empresa';
import { EmpresaService } from '../../services/empresa.service';
import { FormUtils } from '../../shared/form-utils';
import Swal from 'sweetalert2';

@Component({
  selector: 'app-ubicacion',
  standalone: true,
  imports: [FormsModule, ReactiveFormsModule, CommonModule],
  templateUrl: './ubicacion.component.html',
  styleUrl: './ubicacion.component.css'
})
export class UbicacionComponent implements OnInit {

  authService = inject(AuthService);
  ubicacionService = inject(UbicacionService);
  empresaService = inject(EmpresaService);
  fb = inject(FormBuilder);

  ubicacionForm!: FormGroup;

  ubicaciones: Ubicacion[] = [];
  empresas: Empresa[] = [];
  empresasFiltradas: Empresa[] = [];

  esSuperAdmin = false;
  esAdminEmpresa = false;
  editando: boolean = false;
  mostrarNuevo = false;
  ubicacionEditandoId: number | null = null;
  ubicacionSeleccionado: any = null;

  page = 0;
  size = 10;

  totalPages = 0;
  totalElements = 0;

  ngOnInit() {
    this.esSuperAdmin = this.authService.isAdmin();
    this.esAdminEmpresa = this.authService.isAdminEmpresa();
    this.initForm();
    this.cargarUbicaciones();
    this.cargarEmpresas();
  }

  initForm() {
    this.ubicacionForm = this.fb.group({
      id: [''],
      nombre: ['', Validators.required],
      descripcion: ['', Validators.required],
      direccion: ['', Validators.required],
      empresa: [''],
      empresaId: [null, Validators.required],
      activo: [false] // 👈 checkbox
    });
  }

  cargarUbicaciones() {
    this.ubicacionService.getAll(this.page, this.size).subscribe({
      next: (data) => {
        this.ubicaciones = data.content;
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
    this.cargarUbicaciones();
  }

  guardar() {
    if (!FormUtils.esValido(this.ubicacionForm)) {
      const campo = FormUtils.getPrimerCampoInvalido(this.ubicacionForm);
      FormUtils.marcarComoTocados(this.ubicacionForm);
      Swal.fire({
        icon: 'warning',
        title: 'Formulario incompleto',
        text: `Revisa el campo: ${campo}`
      });

      console.log(FormUtils.getErrores(this.ubicacionForm));

      return;
    }

    const ubicacion: Ubicacion = this.ubicacionForm.value;

    if (this.editando && this.ubicacionEditandoId !== null) {
      // EDITAR
      Swal.fire({
        title: '¿Estás seguro?',
        text: 'Esta acción actualizará la ubicación',
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

          this.ubicacionService.update(this.ubicacionEditandoId!, ubicacion).subscribe({
            next: () => {
              Swal.fire({
                icon: 'success',
                title: 'Actualizado',
                text: 'La ubicación fue actualizado correctamente',
                timer: 2000,
                showConfirmButton: false
              });

              this.cargarUbicaciones(); // 🔄 refrescar tabla
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
      this.ubicacionService.create(ubicacion).subscribe({
        next: () => {
          this.resetForm();
          this.cargarUbicaciones();

          Swal.fire({
            icon: 'success',
            title: '¡Guardado!',
            text: 'La ubicación fue creada correctamente',
            confirmButtonColor: '#3498db'
          });
        },
        error: () => {
          Swal.fire({
            icon: 'error',
            title: 'Error',
            text: 'No se pudo guardar la ubicación'
          });
        }
      });
    }
  }

  resetForm() {
    this.ubicacionForm.reset();
    this.editando = false;
    this.ubicacionEditandoId = null;
  }

  nuevo(){
    this.resetForm();
    this.mostrarNuevo = false;
  }
  
  editar(ubicacion: Ubicacion) {
    this.editando = true;
    this.esSuperAdmin = true;
    this.ubicacionEditandoId = ubicacion.id!;
    this.ubicacionSeleccionado = ubicacion!;

    this.ubicacionForm.patchValue({
      id: ubicacion.id,
      nombre: ubicacion.nombre,
      descripcion: ubicacion.descripcion,
      direccion: ubicacion.direccion,
      empresa: ubicacion.empresa.nombre,
      empresaId: ubicacion.empresa.id,
      activo: ubicacion.activo
    });

    if (this.authService.isAdmin() || this.authService.isAdminEmpresa()){
      this.mostrarNuevo = true;
    }  
  }

  confirmarEliminar(id: number) {
    //this.eliminar(id);
  }


}
