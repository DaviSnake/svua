import { Component, inject, OnInit } from '@angular/core';
import { Activo } from '../../model/activo';
import { ActivoService } from '../../services/activo.service';
import { FormBuilder, FormControl, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { TipoActivo } from '../../model/tipoActivo';
import { Proveedor } from '../../model/proveedor';
import { Ubicacion } from '../../model/ubicacion';
import { AuthService } from '../../services/auth.service';
import { Empresa } from '../../model/empresa';
import { EmpresaService } from '../../services/empresa.service';
import { TipoActivoService } from '../../services/tipo-activo.service';
import { UbicacionService } from '../../services/ubicacion.service';
import { ProveedorService } from '../../services/proveedor.service';
import Swal from 'sweetalert2';
import { FormUtils } from '../../shared/form-utils';

@Component({
  selector: 'app-activo',
  standalone: true,
  imports: [FormsModule, ReactiveFormsModule, CommonModule, MatAutocompleteModule],
  templateUrl: './activo.component.html',
  styleUrl: './activo.component.css'
})
export class ActivoComponent implements OnInit {

  activoService = inject(ActivoService);
  authService = inject(AuthService);
  tipoActivoService = inject(TipoActivoService);
  ubicacionService = inject(UbicacionService);
  proveedorService = inject(ProveedorService);
  empresaService = inject(EmpresaService);
  fb = inject(FormBuilder);

  activoForm!: FormGroup;

  // 🔥 Autocompletado tipo "escribir para buscar" (mismo patrón que Calendario.activoControl),
  // igual que la carga masiva manual, pero manteniendo el envío por ID hacia el backend
  // (ActivoCreateRequest/ActivoUpdateRequest siguen esperando tipoActivoId/ubicacionId/proveedorId).
  tipoActivoControl = new FormControl();
  ubicacionControl = new FormControl();
  proveedorControl = new FormControl();
  empresaControl = new FormControl();

  tipoActivosFiltrados: TipoActivo[] = [];
  ubicacionesFiltrados: Ubicacion[] = [];
  proveedoresFiltrados: Proveedor[] = [];

  activos: Activo[] = [];
  tipoActivos: TipoActivo[] = [];
  proveedores: Proveedor[] = [];
  ubicaciones: Ubicacion[] = [];
  empresas: Empresa[] = [];
  empresasFiltradas: Empresa[] = [];

  drawerOpen = false;
  editando: boolean = false;
  mostrarNuevo = false;
  mostrarModalActivo = false;
  activoEditandoId: number | null = null;
  activoSeleccionado: any = null;
  filtro: string = '';

  esSuperAdmin = false;
  esAdminEmpresa = false;
  bloquearCampo = true;

  page = 0;
  size = 10;
  sizeCombo = 50;

  totalPages = 0;
  totalElements = 0;

  ngOnInit() {
    this.esSuperAdmin = this.authService.isAdmin();
    this.esAdminEmpresa = this.authService.isAdminEmpresa();
    this.initForm();
    this.initAutocompletes();
    this.cargarActivos();
    this.cargarTipoActivos();
    this.cargarUbicaciones();
    this.cargarProveedores();
    this.cargarEmpresas();
  }

  initForm() {
    this.activoForm = this.fb.group({
      id: [''],
      codigoInterno: ['', Validators.required],
      nombre: ['',  Validators.required],
      descripcion: ['', Validators.required],
      tipoActivoId: [null, Validators.required],
      tipoActivoNombre: [''],
      marca: ['', Validators.required],
      modelo: ['', Validators.required],
      numeroSerie: ['', Validators.required],
      fechaAdquisicion: ['', Validators.required],
      valorAdquisicion: ['',[Validators.required, Validators.pattern('^[0-9]+$')]],
      valorResidual: ['', [Validators.required, Validators.pattern('^[0-9]+$')]],
      vidaUtilMeses: ['', [Validators.required, Validators.pattern('^[0-9]+$')]],
      ubicacionId: [null, Validators.required],
      ubicacionNombre: [''],
      proveedorId: [null, Validators.required],
      proveedorNombre: [''],
      empresaId: [null, Validators.required],
      estadoActual: [''],
      cuentaContable: [''],

      activo: [true] // 👈 checkbox
    });
  }

  // 🔥 Filtra las 3 listas a medida que se escribe (igual que Calendario.initFiltroActivos)
  // y mantiene sincronizado el id real (tipoActivoId/ubicacionId/proveedorId) que se manda al backend.
  initAutocompletes() {
    this.tipoActivoControl.valueChanges.subscribe(value => {
      const seleccionado = value && typeof value === 'object' ? value : null;
      this.activoForm.patchValue({ tipoActivoId: seleccionado?.id || null });

      const search = (typeof value === 'string' ? value : value?.nombre || '').toLowerCase().trim();
      this.tipoActivosFiltrados = !search
        ? this.tipoActivos
        : this.tipoActivos.filter(t => t.nombre.toLowerCase().includes(search));
    });

    this.ubicacionControl.valueChanges.subscribe(value => {
      const seleccionado = value && typeof value === 'object' ? value : null;
      this.activoForm.patchValue({ ubicacionId: seleccionado?.id || null });

      const search = (typeof value === 'string' ? value : value?.nombre || '').toLowerCase().trim();
      this.ubicacionesFiltrados = !search
        ? this.ubicaciones
        : this.ubicaciones.filter(u => u.nombre.toLowerCase().includes(search));
    });

    this.proveedorControl.valueChanges.subscribe(value => {
      const seleccionado = value && typeof value === 'object' ? value : null;
      this.activoForm.patchValue({ proveedorId: seleccionado?.id || null });

      const search = (typeof value === 'string' ? value : value?.nombre || '').toLowerCase().trim();
      this.proveedoresFiltrados = !search
        ? this.proveedores
        : this.proveedores.filter(p => p.nombre.toLowerCase().includes(search));
    });

    this.empresaControl.valueChanges.subscribe(value => {
      const seleccionado = value && typeof value === 'object' ? value : null;
      this.activoForm.patchValue({ empresaId: seleccionado?.id || null });

      const search = (typeof value === 'string' ? value : value?.nombre || '').toLowerCase().trim();
      this.empresasFiltradas = !search
        ? this.empresas
        : this.empresas.filter(e => e.nombre.toLowerCase().includes(search));
    });
  }

  displayTipoActivo = (tipoActivo: any): string => tipoActivo?.nombre ?? '';
  displayUbicacion = (ubicacion: any): string => ubicacion?.nombre ?? '';
  displayProveedor = (proveedor: any): string => proveedor?.nombre ?? '';
  displayEmpresa = (empresa: any): string => empresa?.nombre ?? '';

  onFocusTipoActivo() {
    // 🔥 al enfocar sin haber escrito nada, muestra todas las opciones
    this.tipoActivosFiltrados = this.tipoActivos;
  }

  onFocusUbicacion() {
    this.ubicacionesFiltrados = this.ubicaciones;
  }

  onFocusProveedor() {
    this.proveedoresFiltrados = this.proveedores;
  }

  onFocusEmpresa() {
    this.empresasFiltradas = this.empresas;
  }

  cargarActivos() {
    this.activoService.getAll(this.page, this.size).subscribe({
      next: (data) => {

        this.activos = data.content;

        this.page = data.page.number;
        this.totalPages = data.page.totalPages;
        this.totalElements = data.page.totalElements;
      },
      error: () => {
        console.log("error");
      }
    });
  }

  cargarTipoActivos() {
    this.tipoActivoService.getTipoActivoCombo(this.page, this.sizeCombo).subscribe({
      next: (data) => {
        this.tipoActivos = data.content;
        this.tipoActivosFiltrados = data.content;
      },
      error: () => {
        console.log("error");
      }
    });
  }

  cargarUbicaciones() {
    this.ubicacionService.getUbicacionCombo(this.page, this.sizeCombo).subscribe({
      next: (data) => {
        this.ubicaciones = data.content;
        this.ubicacionesFiltrados = data.content;
      },
      error: () => {
        console.log("error");
      }
    });
  }

  cargarProveedores() {
    this.proveedorService.getProveedorCombo(this.page, this.sizeCombo).subscribe({
      next: (data) => {
        this.proveedores = data.content;
        this.proveedoresFiltrados = data.content;
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
    this.cargarActivos();
  }

  get activosFiltrados() {
    const f = this.filtro.toLowerCase();

    return this.activos.filter(a =>
      (a.nombre?.toLowerCase().includes(f) ||
      a.codigoInterno?.toLowerCase().includes(f) ||
      a.marca?.toLowerCase().includes(f) ||
      a.numeroSerie?.toLowerCase().includes(f) ||
      a.modelo?.toLowerCase().includes(f))
    );
  }

  nuevo(){
    this.resetForm();
    this.mostrarNuevo = false;
  }

  resetForm() {
    this.activoForm.reset();
    this.tipoActivoControl.reset();
    this.ubicacionControl.reset();
    this.proveedorControl.reset();
    this.empresaControl.reset();
    this.editando = false;
    this.activoEditandoId = null;
  }

  // 🔥 Espera a que el combo correspondiente ya haya cargado (igual que
  // Calendario.setActivoSeleccionado) antes de setear el valor mostrado
  // en el autocompletado, para que muestre el nombre y no quede vacío.
  setTipoActivoSeleccionado(tipoActivoId: number) {
    if (!this.tipoActivos || this.tipoActivos.length === 0) {
      setTimeout(() => this.setTipoActivoSeleccionado(tipoActivoId), 200);
      return;
    }
    const tipoActivo = this.tipoActivos.find(t => t.id === tipoActivoId);
    if (tipoActivo) {
      this.tipoActivosFiltrados = [...this.tipoActivos];
      setTimeout(() => this.tipoActivoControl.setValue(tipoActivo));
    }
  }

  setUbicacionSeleccionada(ubicacionId: number) {
    if (!this.ubicaciones || this.ubicaciones.length === 0) {
      setTimeout(() => this.setUbicacionSeleccionada(ubicacionId), 200);
      return;
    }
    const ubicacion = this.ubicaciones.find(u => u.id === ubicacionId);
    if (ubicacion) {
      this.ubicacionesFiltrados = [...this.ubicaciones];
      setTimeout(() => this.ubicacionControl.setValue(ubicacion));
    }
  }

  setProveedorSeleccionado(proveedorId: number) {
    if (!this.proveedores || this.proveedores.length === 0) {
      setTimeout(() => this.setProveedorSeleccionado(proveedorId), 200);
      return;
    }
    const proveedor = this.proveedores.find(p => p.id === proveedorId);
    if (proveedor) {
      this.proveedoresFiltrados = [...this.proveedores];
      setTimeout(() => this.proveedorControl.setValue(proveedor));
    }
  }

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

  editar(activo: Activo) {
    this.editando = true;
    this.esSuperAdmin = true;
    this.activoEditandoId = activo.id!;
    this.activoSeleccionado = activo!;

    this.activoForm.patchValue({
      codigoInterno: activo.codigoInterno,
      nombre: activo.nombre,
      descripcion: activo.descripcion,
      tipoActivoNombre: activo.tipoActivo.nombre,
      tipoActivoId: activo.tipoActivo.id,
      marca: activo.marca,
      modelo: activo.modelo,
      numeroSerie: activo.numeroSerie,
      fechaAdquisicion: activo.fechaAdquisicion,
      valorAdquisicion: activo.valorAdquisicion,
      valorResidual: activo.valorResidual,
      vidaUtilMeses: activo.vidaUtilMeses,
      ubicacionId: activo.ubicacion.id,
      ubicacionNombre: activo.ubicacion.nombre,
      proveedorId: activo.proveedor.id,
      proveedorNombre: activo.proveedor.nombre,
      empresaId: activo.empresa.id,
      estadoActual: activo.estadoActual,
      cuentaContable: activo.cuentaContable,
    });

    this.setTipoActivoSeleccionado(activo.tipoActivo.id!);
    this.setUbicacionSeleccionada(activo.ubicacion.id!);
    this.setProveedorSeleccionado(activo.proveedor.id!);
    this.setEmpresaSeleccionada(activo.empresa.id!);

    if (this.authService.isAdmin() || this.authService.isAdminEmpresa()){
      this.mostrarNuevo = true;
    }

  }

  guardar() {
    if (!FormUtils.esValido(this.activoForm)) {
      const campo = FormUtils.getPrimerCampoInvalido(this.activoForm);
      FormUtils.marcarComoTocados(this.activoForm);
      Swal.fire({
        icon: 'warning',
        title: 'Formulario incompleto',
        text: `Revisa el campo: ${campo}`
      });

      console.log(FormUtils.getErrores(this.activoForm));

      return;
    }
    const activo: Activo = this.activoForm.value;

    const body = {
      codigoInterno: activo.codigoInterno,
      nombre: activo.nombre,
      descripcion: activo.descripcion,
      tipoActivoId: this.activoForm.value.tipoActivoId,
      marca: activo.marca,
      modelo: activo.modelo,
      numeroSerie: activo.numeroSerie,
      fechaAdquisicion: activo.fechaAdquisicion,
      valorAdquisicion: activo.valorAdquisicion,
      valorResidual: activo.valorResidual,
      vidaUtilMeses: activo.vidaUtilMeses,
      ubicacionId: this.activoForm.value.ubicacionId,
      proveedorId: this.activoForm.value.proveedorId,
      cuentaContable: this.activoForm.value.cuentaContable
    };

    if (this.editando && this.activoEditandoId !== null) {
      // EDITAR
      Swal.fire({
        title: '¿Estás seguro?',
        text: 'Esta acción actualizará el activo',
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

          this.activoService.update(this.activoEditandoId!, activo).subscribe({
            next: () => {
              Swal.fire({
                icon: 'success',
                title: 'Actualizado',
                text: 'El activo fue actualizado correctamente',
                timer: 2000,
                showConfirmButton: false
              });

              this.cargarActivos(); // 🔄 refrescar tabla
              this.resetForm();
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
      this.activoService.create(body).subscribe({
        next: () => {
          this.resetForm();
          this.cargarActivos();

          Swal.fire({
            icon: 'success',
            title: '¡Guardado!',
            text: 'El activo fue creado correctamente',
            confirmButtonColor: '#3498db'
          });
        },
        error: () => {
          Swal.fire({
            icon: 'error',
            title: 'Error',
            text: 'No se pudo guardar el activo'
          });
        }
      });
    }
  }

  darDeBaja(id: number) {
    Swal.fire({
      title: '¿Estás seguro?',
      text: 'Esta acción dará de baja al activo',
      icon: 'warning',
      showCancelButton: true,
      confirmButtonText: 'Sí, actualizar',
      cancelButtonText: 'Cancelar'
    }).then(result => {
      if (result.isConfirmed) {

        Swal.fire({
          title: 'Dar de Baja',
          input: 'textarea',
          inputLabel: 'Motivo de la baja',
          inputPlaceholder: 'Escribe el motivo...',
          showCancelButton: true,

          confirmButtonText: 'Guardar',
          cancelButtonText: 'Cancelar',

          confirmButtonColor: '#3b82f6',
          cancelButtonColor: '#64748b',

          buttonsStyling: true, // 🔥 IMPORTANTE

          inputValidator: (value) => {
            if (!value || value.trim().length < 3) {
              return 'Debes ingresar un motivo válido';
            }
            return null;
          }
        }).then((result) => {

          // ❌ Canceló → volver atrás
        if (!result.isConfirmed) {
          return;
        }

        const motivo = result.value?.trim();

          // ✅ Llamar backend
        this.activoService.darDeBaja(id, motivo)
          .subscribe({
            next: () => {
              Swal.fire({
                icon: 'success',
                title: 'Baja activo',
                text: 'El activo se dio de baja',
                timer: 2000,
                showConfirmButton: false
              });
              this.cargarActivos(); // 🔄 refrescar tabla
            },
            error: () => {
              Swal.fire({
                icon: 'error',
                title: 'Error',
                text: 'No se pudo dar de baja al activo'
              });
            }
          });
        });
      }
    });
  }

  abrirModalActivo(activo: any) {
    this.activoSeleccionado = activo;
    this.mostrarModalActivo = true;

    this.activoForm.patchValue({
      codigoInterno: activo.codigoInterno,
      nombre: activo.nombre,
      descripcion: activo.descripcion,
      tipoActivoNombre: activo.tipoActivo.nombre,
      marca: activo.marca,
      modelo: activo.modelo,
      numeroSerie: activo.numeroSerie,
      fechaAdquisicion: activo.fechaAdquisicion,
      valorAdquisicion: activo.valorAdquisicion,
      valorResidual: activo.valorResidual,
      vidaUtilMeses: activo.vidaUtilMeses,
      ubicacionNombre: activo.ubicacion.nombre,
      proveedorNombre: activo.proveedor.nombre,
      estadoActual: activo.estadoActual,
      cuentaContable: activo.cuentaContable,
    });
    this.activoForm.get('codigoInterno')?.disable(); // 🔥 aquí
    this.activoForm.get('nombre')?.disable(); // 🔥 aquí
    this.activoForm.get('descripcion')?.disable(); // 🔥 aquí
    this.activoForm.get('tipoActivoNombre')?.disable(); // 🔥 aquí
    this.activoForm.get('marca')?.disable(); // 🔥 aquí
    this.activoForm.get('modelo')?.disable(); // 🔥 aquí
    this.activoForm.get('numeroSerie')?.disable(); // 🔥 aquí
    this.activoForm.get('fechaAdquisicion')?.disable(); // 🔥 aquí
    this.activoForm.get('valorAdquisicion')?.disable(); // 🔥 aquí
    this.activoForm.get('valorResidual')?.disable(); // 🔥 aquí
    this.activoForm.get('vidaUtilMeses')?.disable(); // 🔥 aquí
    this.activoForm.get('ubicacionNombre')?.disable(); // 🔥 aquí
    this.activoForm.get('proveedorNombre')?.disable(); // 🔥 aquí
    this.activoForm.get('estadoActual')?.disable(); // 🔥 aquí
    this.activoForm.get('cuentaContable')?.disable(); // 🔥 aquí
  }



  cerrarModal() {
    this.activoForm.get('codigoInterno')?.enable(); // 🔥 aquí
    this.activoForm.get('nombre')?.enable(); // 🔥 aquí
    this.activoForm.get('descripcion')?.enable(); // 🔥 aquí
    this.activoForm.get('tipoActivoNombre')?.enable(); // 🔥 aquí
    this.activoForm.get('marca')?.enable(); // 🔥 aquí
    this.activoForm.get('modelo')?.enable(); // 🔥 aquí
    this.activoForm.get('numeroSerie')?.enable(); // 🔥 aquí
    this.activoForm.get('fechaAdquisicion')?.enable(); // 🔥 aquí
    this.activoForm.get('valorAdquisicion')?.enable(); // 🔥 aquí
    this.activoForm.get('valorResidual')?.enable(); // 🔥 aquí
    this.activoForm.get('vidaUtilMeses')?.enable(); // 🔥 aquí
    this.activoForm.get('ubicacionNombre')?.enable(); // 🔥 aquí
    this.activoForm.get('proveedorNombre')?.enable(); // 🔥 aquí
    this.activoForm.get('estadoActual')?.enable(); // 🔥 aquí
    this.activoForm.get('cuentaContable')?.enable(); // 🔥 aquí
    this.mostrarModalActivo = false;
    this.activoForm.reset();
  }

}
