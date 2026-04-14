import { Component, inject, OnInit } from '@angular/core';
import { Activo } from '../../model/activo';
import { ActivoService } from '../../services/activo.service';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { CommonModule } from '@angular/common';
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
  imports: [FormsModule, ReactiveFormsModule, CommonModule],
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

  totalPages = 0;
  totalElements = 0;

  ngOnInit() {
    this.esSuperAdmin = this.authService.isAdmin();
    this.esAdminEmpresa = this.authService.isAdminEmpresa();
    this.initForm();
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
      activo: [false] // 👈 checkbox
    });
  }

  cargarActivos() {
    this.activoService.getAll(this.page, this.size).subscribe({
      next: (data) => {
        this.activos = data.content;
        this.totalPages = data.totalPages;
        this.totalElements = data.totalElements;
      },
      error: () => {
        console.log("error");
      }
    });
  }

  cargarTipoActivos() {
    this.tipoActivoService.getAll(this.page, this.size).subscribe({
      next: (data) => {
        this.tipoActivos = data.content;
      },
      error: () => {
        console.log("error");
      }
    });
  }

  cargarUbicaciones() {
    this.ubicacionService.getAll(this.page, this.size).subscribe({
      next: (data) => {
        this.ubicaciones = data.content;
      },
      error: () => {
        console.log("error");
      }
    });
  }

  cargarProveedores() {
    this.proveedorService.getAll(this.page, this.size).subscribe({
      next: (data) => {
        this.proveedores = data.content;
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
    this.editando = false;
    this.activoEditandoId = null;
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
    });

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
      this.activoService.create(activo).subscribe({
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

  confirmarEliminar(id: number) {
    this.eliminar(id);
  }

  eliminar(id: number) {
    this.activoService.delete(id).subscribe(() => this.cargarActivos());
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
  }

  cerrarModal() {
    this.mostrarModalActivo = false;
    this.activoForm.reset();
  }

}
