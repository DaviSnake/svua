import { CommonModule } from '@angular/common';
import { Component, inject, OnInit } from '@angular/core';
import { FormBuilder, FormControl, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { Proveedor } from '../../model/proveedor';
import { ActivoService } from '../../services/activo.service';
import { AuthService } from '../../services/auth.service';
import { ProveedorService } from '../../services/proveedor.service';
import { EmpresaService } from '../../services/empresa.service';
import { Empresa } from '../../model/empresa';
import { FormUtils } from '../../shared/form-utils';
import { calcularPaginasVisibles } from '../../shared/pagination.util';
import Swal from 'sweetalert2';

@Component({
  selector: 'app-proveedor',
  standalone: true,
  imports: [FormsModule, ReactiveFormsModule, CommonModule, MatAutocompleteModule],
  templateUrl: './proveedor.component.html',
  styleUrl: './proveedor.component.css'
})
export class ProveedorComponent implements OnInit {

  authService = inject(AuthService);
  proveedorService = inject(ProveedorService);
  empresaService = inject(EmpresaService);
  fb = inject(FormBuilder);

  proveedorForm!: FormGroup;

  // 🔥 Autocompletado "escribir para buscar" (mismo patrón que Activo),
  // manteniendo el envío por ID hacia el backend (empresaId).
  empresaControl = new FormControl();

  // 🔥 "Tipo Proveedor" es una lista fija (no viene de una tabla), pero
  // igual se muestra como autocompletado por consistencia visual con
  // el resto de los combos del formulario.
  tiposProveedor = ['INTERNO', 'EXTERNO'];
  tiposProveedorFiltrados: string[] = this.tiposProveedor;
  tipoProveedorControl = new FormControl();

  proveedores: Proveedor[] = [];
  empresas: Empresa[] = [];
  empresasFiltradas: Empresa[] = [];

  // 🔥 Filtro de la grilla por empresa (mismo patrón de autocompletado;
  // solo tiene efecto real para SUPER_ADMIN, que es el único rol que ve
  // registros de más de una empresa a la vez).
  filtroEmpresaControl = new FormControl();
  empresasFiltroFiltradas: Empresa[] = [];
  filtroEmpresaId: number | null = null;

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
    this.initAutocompletes();
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
      telefono: ['', [Validators.required, Validators.pattern('^[+0-9]+$')]],
      email: ['', [Validators.required, Validators.email]],
      empresa: [''],
      empresaId: [null, Validators.required],
      tipoProveedor: [null, Validators.required],
      activo: [true] // 👈 checkbox
    });
  }

  // 🔥 Filtra la lista de empresas a medida que se escribe y mantiene
  // sincronizado el empresaId real que se manda al backend.
  initAutocompletes() {
    this.empresaControl.valueChanges.subscribe(value => {
      const seleccionada = value && typeof value === 'object' ? value : null;
      this.proveedorForm.patchValue({ empresaId: seleccionada?.id || null });

      const search = (typeof value === 'string' ? value : value?.nombre || '').toLowerCase().trim();
      this.empresasFiltradas = !search
        ? this.empresas
        : this.empresas.filter(e => e.nombre.toLowerCase().includes(search));
    });

    // 🔥 Acá el valor ya es directamente el string ("INTERNO"/"EXTERNO"),
    // no un objeto con id/nombre, así que se sincroniza tal cual.
    this.tipoProveedorControl.valueChanges.subscribe(value => {
      this.proveedorForm.patchValue({ tipoProveedor: value || null });

      const search = (value || '').toLowerCase().trim();
      this.tiposProveedorFiltrados = !search
        ? this.tiposProveedor
        : this.tiposProveedor.filter(t => t.toLowerCase().includes(search));
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
        this.cargarProveedores();
      } else if (!search && this.filtroEmpresaId !== null) {
        this.filtroEmpresaId = null;
        this.page = 0;
        this.cargarProveedores();
      }
    });
  }

  displayEmpresa = (empresa: any): string => empresa?.nombre ?? '';

  onFocusEmpresa() {
    this.empresasFiltradas = this.empresas;
  }

  onFocusTipoProveedor() {
    this.tiposProveedorFiltrados = this.tiposProveedor;
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

  cargarProveedores() {
    this.proveedorService.getAll(this.page, this.size, this.filtroEmpresaId).subscribe({
      next: (data) => {
        this.proveedores = data.content;
        this.totalPages = data.page.totalPages;
        this.totalElements = data.page.totalElements;

        //console.log("DATA:", this.proveedores)
      },
      error: (err) => {
        Swal.fire({
          icon: 'error',
          title: 'Error',
          // 👇 el backend devuelve el mensaje en la propiedad "error", no "message"
          text: err.error?.error || 'Error desconocido'
        });

        console.log("ERROR:", err);
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

  // 🔥 Botones de página a mostrar (con "..." si hay muchas), en vez de
  // listar un botón por cada página.
  paginasVisibles(): number[] {
    return calcularPaginasVisibles(this.page, this.totalPages);
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
                // 👇 el backend devuelve el mensaje en la propiedad "error", no "message"
                // (por ejemplo "Ya existe un proveedor con ese RUT/email")
                text: err.error?.error || 'No se pudo actualizar'
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
        error: (err) => {
          Swal.fire({
            icon: 'error',
            title: 'Error',
            // 👇 muestra el motivo real (ej. RUT/email duplicado) en vez de
            // un mensaje genérico
            text: err.error?.error || 'No se pudo guardar el proveedor'
          });
        }
      });
    }
  }

  resetForm() {
    this.proveedorForm.reset();
    this.empresaControl.reset();
    this.tipoProveedorControl.reset();
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
      tipoProveedor: proveedor.tipoProveedor,
      activo: proveedor.activo
    });

    this.setEmpresaSeleccionada(proveedor.empresa.id!);
    this.tipoProveedorControl.setValue(proveedor.tipoProveedor);

    if (this.authService.isAdmin() || this.authService.isAdminEmpresa()){
      this.mostrarNuevo = true;
    }

  }

  eliminar(id: number) {
    Swal.fire({
      title: '¿Estás seguro?',
      text: 'Esta acción dejará inactivo al proveedor',
      icon: 'warning',
      showCancelButton: true,
      confirmButtonText: 'Sí, eliminar',
      cancelButtonText: 'Cancelar'
    }).then(result => {
      if (result.isConfirmed) {

        // ✅ Llamar backend
        this.proveedorService.delete(id)
          .subscribe({
            next: () => {
              Swal.fire({
                icon: 'success',
                title: 'Eliminar',
                text: 'El proveedor se eliminó correctamente',
                timer: 2000,
                showConfirmButton: false
              });
              this.cargarProveedores(); // 🔄 refrescar tabla
              this.nuevo();
            },
            error: () => {
              Swal.fire({
                icon: 'error',
                title: 'Error',
                text: 'No se pudo eliminar al proveedor'
              });
            }
          });
      }
    });
  }


  // 🔥 En la BD el RUT se guarda sin puntos (ej: "12345678-9"); acá
  // solo se formatea para mostrarlo en la grilla (ej: "12.345.678-9").
  formatearRut(rut: string): string {

    if (!rut) {
      return '';
    }

    const [cuerpo, dv] = rut.split('-');

    // Si no viene con el guión del dígito verificador, se muestra tal
    // cual en vez de romper (dato legado con formato inesperado).
    if (!dv || isNaN(Number(cuerpo))) {
      return rut;
    }

    return `${Number(cuerpo).toLocaleString('es-CL')}-${dv.toUpperCase()}`;
  }

  // 🔥 trackBy para la tabla principal de proveedores.
  trackByProveedorId(index: number, proveedor: any): any {
    return proveedor?.id ?? index;
  }

}
