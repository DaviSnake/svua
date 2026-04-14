import { CommonModule } from '@angular/common';
import { Component, inject, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { Proveedor } from '../../model/proveedor';
import { ActivoService } from '../../services/activo.service';
import { AuthService } from '../../services/auth.service';
import { ProveedorService } from '../../services/proveedor.service';
import { EmpresaService } from '../../services/empresa.service';
import { Empresa } from '../../model/empresa';
import { FormUtils } from '../../shared/form-utils';
import Swal from 'sweetalert2';

@Component({
  selector: 'app-proveedor',
  standalone: true,
  imports: [FormsModule, ReactiveFormsModule, CommonModule],
  templateUrl: './proveedor.component.html',
  styleUrl: './proveedor.component.css'
})
export class ProveedorComponent implements OnInit {

  authService = inject(AuthService);
  proveedorService = inject(ProveedorService);
  empresaService = inject(EmpresaService);
  fb = inject(FormBuilder);

  proveedorForm!: FormGroup;

  proveedores: Proveedor[] = [];
  empresas: Empresa[] = [];
  empresasFiltradas: Empresa[] = [];

  esSuperAdmin = false;
  esAdminEmpresa = false;
  editando: boolean = false;
  mostrarNuevo = false;
  proveedorEditandoId: number | null = null;
  proveedorSeleccionado: any = null;

  page = 0;
  size = 10;

  totalPages = 0;
  totalElements = 0;

  ngOnInit() {
    this.esSuperAdmin = this.authService.isAdmin();
    this.esAdminEmpresa = this.authService.isAdminEmpresa();
    this.initForm();
    this.cargarProveedores();
    this.cargarEmpresas();
  }

  initForm() {
    this.proveedorForm = this.fb.group({
      id: [''],
      nombre: ['', Validators.required],
      rut: ['', Validators.required],
      //rut: ['', [Validators.required, rutValidator]],
      contacto: ['', Validators.required],
      telefono: ['', [Validators.required, Validators.pattern('^[0-9]+$')]],
      email: ['', [Validators.required, Validators.email]],
      empresa: [''],
      empresaId: [null, Validators.required],
      activo: [false] // 👈 checkbox
    });
  }

  cargarProveedores() {
    this.proveedorService.getAll(this.page, this.size).subscribe({
      next: (data) => {
        this.proveedores = data.content;
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
    this.cargarProveedores();
  }

   guardar() {
    if (!FormUtils.esValido(this.proveedorForm)) {
      const campo = FormUtils.getPrimerCampoInvalido(this.proveedorForm);
      FormUtils.marcarComoTocados(this.proveedorForm);
      Swal.fire({
        icon: 'warning',
        title: 'Formulario incompleto',
        text: `Revisa el campo: ${campo}`
      });

      console.log(FormUtils.getErrores(this.proveedorForm));

      return;
    }

    const proveedor: Proveedor = this.proveedorForm.value;

    if (this.editando && this.proveedorEditandoId !== null) {
      // EDITAR
      Swal.fire({
        title: '¿Estás seguro?',
        text: 'Esta acción actualizará el proveedor',
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

          this.proveedorService.update(this.proveedorEditandoId!, proveedor).subscribe({
            next: () => {
              Swal.fire({
                icon: 'success',
                title: 'Actualizado',
                text: 'El proveedor fue actualizado correctamente',
                timer: 2000,
                showConfirmButton: false
              });

              this.cargarProveedores(); // 🔄 refrescar tabla
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
      this.proveedorService.create(proveedor).subscribe({
        next: () => {
          this.resetForm();
          this.cargarProveedores();

          Swal.fire({
            icon: 'success',
            title: '¡Guardado!',
            text: 'El proveedor fue creado correctamente',
            confirmButtonColor: '#3498db'
          });
        },
        error: () => {
          Swal.fire({
            icon: 'error',
            title: 'Error',
            text: 'No se pudo guardar el proveedor'
          });
        }
      });
    }
  }

  resetForm() {
    this.proveedorForm.reset();
    this.editando = false;
    this.proveedorEditandoId = null;
  }

  nuevo(){
    this.resetForm();
    this.mostrarNuevo = false;
  }

  editar(proveedor: Proveedor) {
    this.editando = true;
    this.esSuperAdmin = true;
    this.proveedorEditandoId = proveedor.id!;
    this.proveedorSeleccionado = proveedor!;

    this.proveedorForm.patchValue({
      nombre: proveedor.nombre,
      rut: proveedor.rut,
      contacto: proveedor.contacto,
      telefono: proveedor.telefono,
      email: proveedor.email,
      empresa: proveedor.empresa.nombre,
      empresaId: proveedor.empresa.id,
      activo: proveedor.activo
    });

    if (this.authService.isAdmin() || this.authService.isAdminEmpresa()){
      this.mostrarNuevo = true;
    }

  }

  confirmarEliminar(id: number) {
    //this.eliminar(id);
  }

}
