import { CommonModule } from '@angular/common';
import { Component, inject, OnInit } from '@angular/core';
import { FormBuilder, FormControl, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { AuthService } from '../../services/auth.service';
import { RepuestoService } from '../../services/repuesto.service';
import { EmpresaService } from '../../services/empresa.service';
import { Empresa } from '../../model/empresa';
import { Repuesto } from '../../model/repuesto';
import { FormUtils } from '../../shared/form-utils';
import Swal from 'sweetalert2';

@Component({
  selector: 'app-repuesto',
  standalone: true,
  imports: [FormsModule, ReactiveFormsModule, CommonModule, MatAutocompleteModule],
  templateUrl: './repuesto.component.html',
  styleUrl: './repuesto.component.css'
})
export class RepuestoComponent implements OnInit {

  authService = inject(AuthService);
  repuestoService = inject(RepuestoService);
  empresaService = inject(EmpresaService);
  fb = inject(FormBuilder);

  repuestoForm!: FormGroup;

  // 🔥 Autocompletado "escribir para buscar" (mismo patrón que Activo),
  // manteniendo el envío por ID hacia el backend (empresaId).
  empresaControl = new FormControl();

  // 🔥 "Tipo Repuesto" es una lista fija (no viene de una tabla), pero
  // igual se muestra como autocompletado por consistencia visual con
  // el resto de los combos del formulario.
  tiposRepuesto = ['REPUESTO', 'FUNGIBLE'];
  tiposRepuestoFiltrados: string[] = this.tiposRepuesto;
  tipoRepuestoControl = new FormControl();

  repuestos: Repuesto[] = [];
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
  repuestoEditandoId: number | null = null;
  repuestoSeleccionado: any = null;

  page = 0;
  size = 10;

  totalPages = 0;
  totalElements = 0;

  ngOnInit() {
    this.esSuperAdmin = this.authService.isAdmin();
    this.esAdminEmpresa = this.authService.isAdminEmpresa();
    this.initForm();
    this.initAutocompletes();
    this.cargarRepuestos();
    this.cargarEmpresas();
  }

  initForm() {
    this.repuestoForm = this.fb.group({
      id: [''],
      codigo: ['', Validators.required],
      nombre: ['', Validators.required],
      descripcion: ['', Validators.required],
      cuentaContable: ['', Validators.required],
      costoUnitario: ['', [Validators.required, Validators.pattern('^[0-9]+$')]],
      stockActual: ['', [Validators.required, Validators.pattern('^[0-9]+$')]],
      stockMinimo: ['', [Validators.required, Validators.pattern('^[0-9]+$')]],
      tipoRepuesto: [null, Validators.required],
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
      this.repuestoForm.patchValue({ empresaId: seleccionada?.id || null });

      const search = (typeof value === 'string' ? value : value?.nombre || '').toLowerCase().trim();
      this.empresasFiltradas = !search
        ? this.empresas
        : this.empresas.filter(e => e.nombre.toLowerCase().includes(search));
    });

    // 🔥 Acá el valor ya es directamente el string ("REPUESTO"/"FUNGIBLE"),
    // no un objeto con id/nombre, así que se sincroniza tal cual.
    this.tipoRepuestoControl.valueChanges.subscribe(value => {
      this.repuestoForm.patchValue({ tipoRepuesto: value || null });

      const search = (value || '').toLowerCase().trim();
      this.tiposRepuestoFiltrados = !search
        ? this.tiposRepuesto
        : this.tiposRepuesto.filter(t => t.toLowerCase().includes(search));
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
        this.cargarRepuestos();
      } else if (!search && this.filtroEmpresaId !== null) {
        this.filtroEmpresaId = null;
        this.page = 0;
        this.cargarRepuestos();
      }
    });
  }

  displayEmpresa = (empresa: any): string => empresa?.nombre ?? '';

  onFocusEmpresa() {
    this.empresasFiltradas = this.empresas;
  }

  onFocusTipoRepuesto() {
    this.tiposRepuestoFiltrados = this.tiposRepuesto;
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

  cargarRepuestos() {
    this.repuestoService.getAll(this.page, this.size, this.filtroEmpresaId).subscribe({
      next: (data) => {
        this.repuestos = data.content;
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
    this.cargarRepuestos();
  }

  guardar() {
    if (!FormUtils.esValido(this.repuestoForm)) {
      const campo = FormUtils.getPrimerCampoInvalido(this.repuestoForm);
      FormUtils.marcarComoTocados(this.repuestoForm);
      Swal.fire({
        icon: 'warning',
        title: 'Formulario incompleto',
        text: `Revisa el campo: ${campo}`
      });

      console.log(FormUtils.getErrores(this.repuestoForm));

      return;
    }

    const repuesto: Repuesto = this.repuestoForm.value;

    if (this.editando && this.repuestoEditandoId !== null) {
      // EDITAR
      Swal.fire({
        title: '¿Estás seguro?',
        text: 'Esta acción actualizará el repuesto',
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

          this.repuestoService.update(this.repuestoEditandoId!, repuesto).subscribe({
            next: () => {
              Swal.fire({
                icon: 'success',
                title: 'Actualizado',
                text: 'El repuesto fue actualizado correctamente',
                timer: 2000,
                showConfirmButton: false
              });

              this.cargarRepuestos(); // 🔄 refrescar tabla
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
      this.repuestoService.create(repuesto).subscribe({
        next: () => {

          this.nuevo();
          this.cargarRepuestos();

          Swal.fire({
            icon: 'success',
            title: '¡Guardado!',
            text: 'El repuesto fue creado correctamente',
            confirmButtonColor: '#3498db'
          });
        },

        error: (err) => {
          console.log("Error: "+ err);

          Swal.fire({
            icon: 'error',
            title: 'Error',
            text: err?.error?.error || 'No se pudo guardar el repuesto'
          });
        }
      });
    }
  }

  resetForm() {
    this.repuestoForm.reset();
    this.empresaControl.reset();
    this.tipoRepuestoControl.reset();
    this.editando = false;
    this.repuestoEditandoId = null;
  }

  nuevo(){
    this.resetForm();
    this.mostrarNuevo = false;
  }

  editar(repuesto: Repuesto) {
    this.editando = true;
    this.esSuperAdmin = true;
    this.repuestoEditandoId = repuesto.id!;
    this.repuestoSeleccionado = repuesto!;

    this.repuestoForm.patchValue({
      id: repuesto.id,
      codigo: repuesto.codigo,
      nombre: repuesto.nombre,
      descripcion: repuesto.descripcion,
      cuentaContable: repuesto.cuentaContable,
      costoUnitario: repuesto.costoUnitario,
      stockActual: repuesto.stockActual,
      stockMinimo: repuesto.stockMinimo,
      tipoRepuesto: repuesto.tipoRepuesto,
      empresaId: repuesto.empresa.id,
      activo: repuesto.activo
    });

    this.setEmpresaSeleccionada(repuesto.empresa.id!);
    this.tipoRepuestoControl.setValue(repuesto.tipoRepuesto);

    if (this.authService.isAdmin() || this.authService.isAdminEmpresa()){
      this.mostrarNuevo = true;
    }
  }

  eliminar(id: number) {
    Swal.fire({
      title: '¿Estás seguro?',
      text: 'Esta acción dejará inactivo el repuesto',
      icon: 'warning',
      showCancelButton: true,
      confirmButtonText: 'Sí, eliminar',
      cancelButtonText: 'Cancelar'
    }).then(result => {
      if (result.isConfirmed) {

        // ✅ Llamar backend
        this.repuestoService.delete(id)
          .subscribe({
            next: () => {
              Swal.fire({
                icon: 'success',
                title: 'Eliminar',
                text: 'El repuesto se eliminó correctamente',
                timer: 2000,
                showConfirmButton: false
              });
              this.cargarRepuestos(); // 🔄 refrescar tabla
              this.nuevo();
            },
            error: () => {    
              Swal.fire({
                icon: 'error',
                title: 'Error',
                text: 'No se pudo eliminar el repuesto'
              });
            }
          });       
      }
    });
  }

}
