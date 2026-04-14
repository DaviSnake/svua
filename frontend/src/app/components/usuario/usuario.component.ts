import { Component, inject, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { Usuario } from '../../model/usuario';
import { CommonModule } from '@angular/common';
import { UsuarioService } from '../../services/usuario.service';
import { AuthService } from '../../services/auth.service';
import { EmpresaService } from '../../services/empresa.service';
import { Empresa } from '../../model/empresa';
import Swal from 'sweetalert2';
import { FormUtils } from '../../shared/form-utils';

@Component({
  selector: 'app-usuario',
  standalone: true,
  imports: [FormsModule, ReactiveFormsModule, CommonModule],
  templateUrl: './usuario.component.html',
  styleUrl: './usuario.component.css'
})
export class UsuarioComponent implements OnInit {

  usuarioService = inject(UsuarioService);
  empresaService = inject(EmpresaService);
  authService = inject(AuthService);
  fb = inject(FormBuilder);

  usuarioForm!: FormGroup;
  usuarios: Usuario[] = [];
  usuariosFiltrados: Usuario[] = [];
  empresas: Empresa[] = [];
  empresasFiltradas: Empresa[] = [];

  esSuperAdmin = false;
  esAdminEmpresa = false;
  mostrarNuevo = false;
  mostrarModalPassword = false;
  usuarioSeleccionado: any = null;

  passwordForm!: FormGroup;

  editando: boolean = false;
  usuarioEditandoId: number | null = null;

  roles = ['SUPER_ADMIN', 'ADMIN_EMPRESA', 'JEFE_MANTENIMIENTO', 'TECNICO', 'BODEGUERO', 'USUARIO'];
  rolesFiltrados: string[] = [];

  page = 0;
  size = 10;

  totalPages = 0;
  totalElements = 0;

  ngOnInit(): void {
    this.esSuperAdmin = this.authService.isAdmin();
    this.esAdminEmpresa = this.authService.isAdminEmpresa();

    this.cargarRolesFiltrados();

    this.initForm();

    this.cargarUsuarios();

    this.cargarEmpresas();

    this.passwordForm = this.fb.group({
      currentPassword: ['', Validators.required],
      password: ['', [Validators.required, Validators.minLength(6)]],
      confirmPassword: ['', Validators.required]
    });
  }

  initForm() {
    this.usuarioForm = this.fb.group({
      nombre: ['', Validators.required],
      email: ['', [Validators.required, Validators.email]],
      password: ['', Validators.minLength(6)],
      empresaId: [null, Validators.required],
      empresaNombre: [null],
      rol: ['USUARIO', Validators.required],
      activo: [false] // 👈 checkbox
    });
  }

  cargarRolesFiltrados(){
    this.rolesFiltrados = this.esSuperAdmin
    ? this.roles
    : this.roles.filter(r => r !== 'SUPER_ADMIN');
  }

  cargarUsuarios() {
    this.usuarioService.getAll(this.page, this.size).subscribe(data => {
      this.usuarios = data.content;
      this.usuariosFiltrados = data.content;
      this.totalPages = data.totalPages;
      this.totalElements = data.totalElements;
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
    this.cargarUsuarios();
  }

  guardar() {
    if (!FormUtils.esValido(this.usuarioForm)) {
      const campo = FormUtils.getPrimerCampoInvalido(this.usuarioForm);
      FormUtils.marcarComoTocados(this.usuarioForm);
      Swal.fire({
        icon: 'warning',
        title: 'Formulario incompleto',
        text: `Revisa el campo: ${campo}`
      });

      console.log(FormUtils.getErrores(this.usuarioForm));

      return;
    }

    const usuario: Usuario = this.usuarioForm.value;

    if (this.editando && this.usuarioEditandoId !== null) {
      // EDITAR
      Swal.fire({
        title: '¿Estás seguro?',
        text: 'Esta acción actualizará el usuario',
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

          this.usuarioService.update(this.usuarioEditandoId!, usuario).subscribe({
            next: () => {
              Swal.fire({
                icon: 'success',
                title: 'Actualizado',
                text: 'El usuario fue actualizado correctamente',
                timer: 2000,
                showConfirmButton: false
              });

              this.cargarUsuarios(); // 🔄 refrescar tabla
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
      this.usuarioService.create(usuario).subscribe({
        next: () => {
          this.resetForm();
          this.cargarUsuarios();

          Swal.fire({
            icon: 'success',
            title: '¡Guardado!',
            text: 'El usuario fue creado correctamente',
            confirmButtonColor: '#3498db'
          });
        },
        error: () => {
          Swal.fire({
            icon: 'error',
            title: 'Error',
            text: 'No se pudo guardar el usuario'
          });
        }
      });
    }
  }

  nuevo(){
    this.resetForm();
    this.mostrarNuevo = false;
  }

  editar(usuario: Usuario) {
    this.editando = true;
    this.esSuperAdmin = true;
    this.usuarioEditandoId = usuario.id!;
    this.usuarioSeleccionado = usuario!;

    this.usuarioForm.patchValue({
      nombre: usuario.nombre,
      email: usuario.email,
      empresaId: usuario.empresaId,
      password: usuario.password,
      rol: usuario.rol,
      activo: usuario.activo
    });

    if (this.authService.isAdmin() || this.authService.isAdminEmpresa()){
      this.mostrarNuevo = true;
    }
  }

  confirmarEliminar(id: number) {
    this.eliminar(id);
  }

  eliminar(id: number) {
    Swal.fire({
      title: '¿Estás seguro?',
      text: 'Esta acción eliminará el usuario',
      icon: 'warning',
      showCancelButton: true,
      confirmButtonText: 'Sí, eliminar',
      cancelButtonText: 'Cancelar'
    }).then(result => {
      if (result.isConfirmed) {

        Swal.fire({
          title: 'Eliminando...',
          allowOutsideClick: false,
          didOpen: () => Swal.showLoading()
        });

        this.usuarioService.delete(id).subscribe({
          next: () => {
            Swal.fire({
              icon: 'success',
              title: 'Eliminado',
              text: 'El usuario fue eliminado correctamente',
              timer: 2000,
              showConfirmButton: false
            });

            this.cargarUsuarios(); // 🔄 refrescar tabla
          },
          error: (err) => {
            console.log(err.error); // 👈 DEBUG
            Swal.fire({
              icon: 'error',
              title: 'Error',
              text: err.error?.message || 'No se pudo eliminar'
            });
          }
        });

      }
    });
  }

  resetForm() {
    this.usuarioForm.reset({ rol: 'USUARIO' });
    this.editando = false;
    this.usuarioEditandoId = null;
  }

  get f() {
    return this.usuarioForm.controls;
  }

  abrirModalPassword(usuario: any) {
    this.usuarioSeleccionado = usuario;
    this.mostrarModalPassword = true;
  }

  cerrarModal() {
    this.mostrarModalPassword = false;
    this.passwordForm.reset();
  }

  guardarPassword() {
    if (this.passwordForm.invalid || !this.passwordsIguales()) {
      this.passwordForm.markAllAsTouched();
      return;
    }

    const data = {
      currentPassword: this.passwordForm.value.currentPassword,
      newPassword: this.passwordForm.value.password
    };

    this.usuarioService.cambiarPassword(this.usuarioSeleccionado.id, data).subscribe(() => {
        this.resetForm();
        this.cargarUsuarios();
      });

    this.cerrarModal();
  }

  passwordsIguales(): boolean {
    return this.passwordForm.value.password === this.passwordForm.value.confirmPassword;
  }


}
