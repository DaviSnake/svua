import { Component, inject, OnInit } from '@angular/core';
import { FormBuilder, FormControl, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { Usuario } from '../../model/usuario';
import { CommonModule } from '@angular/common';
import { UsuarioService } from '../../services/usuario.service';
import { AuthService } from '../../services/auth.service';
import { EmpresaService } from '../../services/empresa.service';
import { Empresa } from '../../model/empresa';
import Swal from 'sweetalert2';
import { FormUtils } from '../../shared/form-utils';
import { Router } from '@angular/router';

@Component({
  selector: 'app-usuario',
  standalone: true,
  imports: [FormsModule, ReactiveFormsModule, CommonModule, MatAutocompleteModule],
  templateUrl: './usuario.component.html',
  styleUrl: './usuario.component.css'
})
export class UsuarioComponent implements OnInit {

  usuarioService = inject(UsuarioService);
  empresaService = inject(EmpresaService);
  authService = inject(AuthService);
  router = inject(Router);
  fb = inject(FormBuilder);

  usuarioForm!: FormGroup;

  // 🔥 Autocompletado "escribir para buscar" (mismo patrón que Activo),
  // manteniendo el envío por ID hacia el backend (empresaId).
  empresaControl = new FormControl();

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

  message = '';
  errorMessage = '';

  totalPages = 0;
  totalElements = 0;

  ngOnInit(): void {
    this.esSuperAdmin = this.authService.isAdmin();
    this.esAdminEmpresa = this.authService.isAdminEmpresa();

    this.cargarRolesFiltrados();

    this.initForm();
    this.initAutocompletes();

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
      confirmarPassword: ['', Validators.minLength(6)],
      empresaId: [null, Validators.required],
      empresaNombre: [null],
      rol: ['USUARIO', Validators.required],
      activo: [false] // 👈 checkbox
    });
  }

  // 🔥 Filtra la lista de empresas a medida que se escribe y mantiene
  // sincronizado el empresaId real que se manda al backend.
  initAutocompletes() {
    this.empresaControl.valueChanges.subscribe(value => {
      const seleccionada = value && typeof value === 'object' ? value : null;
      this.usuarioForm.patchValue({ empresaId: seleccionada?.id || null });

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

  cargarRolesFiltrados(){
    this.rolesFiltrados = this.esSuperAdmin
    ? this.roles
    : this.roles.filter(r => r !== 'SUPER_ADMIN');
  }

  cargarUsuarios() {
    this.usuarioService.getAll(this.page, this.size).subscribe(data => {
      this.usuarios = data.content;
      this.usuariosFiltrados = data.content;
      this.totalPages = data.page.totalPages;
      this.totalElements = data.page.totalElements;
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

    if (usuario.password !== this.usuarioForm.value.confirmarPassword && !this.editando){
      Swal.fire({
        title: 'Validación',
        text: 'Los Password deben coincidir',
        icon: 'error',
        timer: 1500,
        showConfirmButton: false
      });
      return;
    }

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

    this.setEmpresaSeleccionada(usuario.empresaId);

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
    this.empresaControl.reset();
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

  enviarCorreo(){
    const data = {
      email: this.usuarioForm.value.email
    };

    Swal.fire({
      icon: 'success',
      title: 'Correo enviado',
      text: `Se ha enviado un correo a ${this.usuarioForm.value.email} para restablecer la contraseña. La aplicación se reiniciará.`,
      timer: 5000,
      showConfirmButton: false
    });

    this.authService.forgotPassword(data).subscribe({
      next: (res: any) => {
        this.message = res.message || 'Revisa tu correo 📩';
        this.errorMessage = '';

        this.message = 'Correo enviado 📩';

        this.goBack();
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Error al enviar correo';
        this.message = '';

        setTimeout(() => this.errorMessage = '', 4000);
      }
    });

  }

  goBack() {
    this.router.navigateByUrl('/login');
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
