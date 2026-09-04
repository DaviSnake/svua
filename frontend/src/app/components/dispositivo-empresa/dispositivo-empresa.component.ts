import { CommonModule } from '@angular/common';
import { Component, inject, OnInit } from '@angular/core';
import { FormBuilder, FormControl, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { debounceTime, distinctUntilChanged } from 'rxjs';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { DispositivoEmpresaService } from '../../services/dispositivo-empresa.service';
import { DispositivoEmpresa } from '../../model/dispositivo-empresa';
import { EmpresaService } from '../../services/empresa.service';
import { Empresa } from '../../model/empresa';
import { FormUtils } from '../../shared/form-utils';
import { calcularPaginasVisibles } from '../../shared/pagination.util';
import Swal from 'sweetalert2';

// 🔒 Pantalla exclusiva de SUPER_ADMIN (ver DispositivoEmpresaController
// y app.routes.ts): que dispositivo fisico de monitoreo (ej. el
// "Dispositivo: INS-877" de un reporte importado por correo) alimenta a
// que empresa. Mismo patron de grilla+formulario que BodegaComponent,
// simplificado porque aca no hay variante ADMIN_EMPRESA.
@Component({
  selector: 'app-dispositivo-empresa',
  standalone: true,
  imports: [FormsModule, ReactiveFormsModule, CommonModule, MatAutocompleteModule],
  templateUrl: './dispositivo-empresa.component.html',
  styleUrl: './dispositivo-empresa.component.css'
})
export class DispositivoEmpresaComponent implements OnInit {

  dispositivoService = inject(DispositivoEmpresaService);
  empresaService = inject(EmpresaService);
  fb = inject(FormBuilder);

  dispositivoForm!: FormGroup;

  empresaControl = new FormControl();

  dispositivos: DispositivoEmpresa[] = [];
  empresas: Empresa[] = [];
  empresasFiltradas: Empresa[] = [];

  // 🔍 Filtro de la grilla por empresa y por texto.
  filtroEmpresaControl = new FormControl();
  empresasFiltroFiltradas: Empresa[] = [];
  filtroEmpresaId: number | null = null;

  busquedaControl = new FormControl('');
  busqueda: string = '';

  editando = false;
  mostrarNuevo = false;
  dispositivoEditandoId: number | null = null;

  page = 0;
  size = 10;

  totalPages = 0;
  totalElements = 0;

  ngOnInit() {
    this.initForm();
    this.initAutocompletes();
    this.cargarDispositivos();
    this.cargarEmpresas();
  }

  initForm() {
    this.dispositivoForm = this.fb.group({
      codigoDispositivo: ['', Validators.required],
      descripcion: [''],
      empresaId: [null, Validators.required]
    });
  }

  initAutocompletes() {
    this.empresaControl.valueChanges.subscribe(value => {
      const seleccionada = value && typeof value === 'object' ? value : null;
      this.dispositivoForm.patchValue({ empresaId: seleccionada?.id || null });

      const search = (typeof value === 'string' ? value : value?.nombre || '').toLowerCase().trim();
      this.empresasFiltradas = !search
        ? this.empresas
        : this.empresas.filter(e => e.nombre.toLowerCase().includes(search));
    });

    this.filtroEmpresaControl.valueChanges.subscribe(value => {
      const esObjeto = value && typeof value === 'object';
      const search = (esObjeto ? value.nombre : value || '').toLowerCase().trim();

      this.empresasFiltroFiltradas = !search
        ? this.empresas
        : this.empresas.filter(e => e.nombre.toLowerCase().includes(search));

      if (esObjeto) {
        this.filtroEmpresaId = value.id;
        this.page = 0;
        this.cargarDispositivos();
      } else if (!search && this.filtroEmpresaId !== null) {
        this.filtroEmpresaId = null;
        this.page = 0;
        this.cargarDispositivos();
      }
    });

    this.busquedaControl.valueChanges
      .pipe(debounceTime(400), distinctUntilChanged())
      .subscribe(value => {
        this.busqueda = (value || '').trim();
        this.page = 0;
        this.cargarDispositivos();
      });
  }

  displayEmpresa = (empresa: any): string => empresa?.nombre ?? '';

  onFocusEmpresa() {
    this.empresasFiltradas = this.empresas;
  }

  onFocusFiltroEmpresa() {
    this.empresasFiltroFiltradas = this.empresas;
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

  cargarDispositivos() {
    this.dispositivoService.getAll(this.page, this.size, this.filtroEmpresaId, this.busqueda).subscribe({
      next: (data) => {
        this.dispositivos = data.content;
        this.totalPages = data.page.totalPages;
        this.totalElements = data.page.totalElements;
      },
      error: () => {
        console.log('error');
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
    this.cargarDispositivos();
  }

  paginasVisibles(): number[] {
    return calcularPaginasVisibles(this.page, this.totalPages);
  }

  guardar() {
    if (!FormUtils.esValido(this.dispositivoForm)) {
      FormUtils.marcarComoTocados(this.dispositivoForm);
      return;
    }

    const dispositivo: DispositivoEmpresa = this.dispositivoForm.value;

    if (this.editando && this.dispositivoEditandoId !== null) {
      Swal.fire({
        title: '¿Estás seguro?',
        text: 'Esta acción actualizará el dispositivo',
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

          this.dispositivoService.update(this.dispositivoEditandoId!, dispositivo).subscribe({
            next: () => {
              Swal.fire({
                icon: 'success',
                title: 'Actualizado',
                text: 'El dispositivo fue actualizado correctamente',
                timer: 2000,
                showConfirmButton: false
              });

              this.cargarDispositivos();
              this.nuevo();
            },
            error: (err) => {
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
      this.dispositivoService.create(dispositivo).subscribe({
        next: () => {
          this.nuevo();
          this.cargarDispositivos();

          Swal.fire({
            icon: 'success',
            title: '¡Guardado!',
            text: 'El dispositivo fue registrado correctamente',
            confirmButtonColor: '#3498db'
          });
        },
        error: (err) => {
          Swal.fire({
            icon: 'error',
            title: 'Error',
            text: err.error?.error || 'No se pudo guardar el dispositivo'
          });
        }
      });
    }
  }

  resetForm() {
    this.dispositivoForm.reset();
    this.empresaControl.reset();
    this.editando = false;
    this.dispositivoEditandoId = null;
  }

  nuevo() {
    this.resetForm();
    this.mostrarNuevo = false;
  }

  editar(dispositivo: DispositivoEmpresa) {
    this.editando = true;
    this.dispositivoEditandoId = dispositivo.id!;
    this.mostrarNuevo = true;

    this.dispositivoForm.patchValue({
      codigoDispositivo: dispositivo.codigoDispositivo,
      descripcion: dispositivo.descripcion,
      empresaId: dispositivo.empresaId
    });

    this.setEmpresaSeleccionada(dispositivo.empresaId!);
  }

  eliminar(id: number) {
    Swal.fire({
      title: '¿Estás seguro?',
      text: 'Esta acción deshabilitará el dispositivo (dejará de importar sus lecturas)',
      icon: 'warning',
      showCancelButton: true,
      confirmButtonText: 'Sí, deshabilitar',
      cancelButtonText: 'Cancelar'
    }).then(result => {
      if (result.isConfirmed) {
        this.dispositivoService.delete(id).subscribe({
          next: () => {
            Swal.fire({
              icon: 'success',
              title: 'Deshabilitado',
              text: 'El dispositivo quedó deshabilitado',
              timer: 2000,
              showConfirmButton: false
            });
            this.cargarDispositivos();
            this.nuevo();
          },
          error: () => {
            Swal.fire({
              icon: 'error',
              title: 'Error',
              text: 'No se pudo deshabilitar el dispositivo'
            });
          }
        });
      }
    });
  }

  habilitar(id: number) {
    this.dispositivoService.habilitar(id).subscribe({
      next: () => {
        Swal.fire({
          icon: 'success',
          title: 'Habilitado',
          timer: 1500,
          showConfirmButton: false
        });
        this.cargarDispositivos();
      },
      error: () => {
        Swal.fire({
          icon: 'error',
          title: 'Error',
          text: 'No se pudo habilitar el dispositivo'
        });
      }
    });
  }

  trackByDispositivoId(index: number, dispositivo: any): any {
    return dispositivo?.id ?? index;
  }
}
