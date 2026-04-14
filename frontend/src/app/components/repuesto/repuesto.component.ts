import { CommonModule } from '@angular/common';
import { Component, inject, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
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
  imports: [FormsModule, ReactiveFormsModule, CommonModule],
  templateUrl: './repuesto.component.html',
  styleUrl: './repuesto.component.css'
})
export class RepuestoComponent implements OnInit {

  authService = inject(AuthService);
  repuestoService = inject(RepuestoService);
  empresaService = inject(EmpresaService);
  fb = inject(FormBuilder);

  repuestoForm!: FormGroup;

  repuestos: Repuesto[] = [];
  empresas: Empresa[] = [];
  empresasFiltradas: Empresa[] = [];

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
    this.cargarRepuestos();
    this.cargarEmpresas();
  }

  initForm() {
    this.repuestoForm = this.fb.group({
      id: [''],
      codigo: ['', Validators.required],
      nombre: ['', Validators.required],
      descripcion: ['', Validators.required],
      costoUnitario: ['', [Validators.required, Validators.pattern('^[0-9]+$')]],
      stockMinimo: ['', [Validators.required, Validators.pattern('^[0-9]+$')]],
      empresaId: [null, Validators.required],
      empresa: [''],
      activo: [false] // 👈 checkbox
    });
  }

  cargarRepuestos() {
    this.repuestoService.getAll(this.page, this.size).subscribe({
      next: (data) => {
        this.repuestos = data.content;
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
          this.resetForm();
          this.cargarRepuestos();

          Swal.fire({
            icon: 'success',
            title: '¡Guardado!',
            text: 'El repuesto fue creado correctamente',
            confirmButtonColor: '#3498db'
          });
        },
        error: () => {
          Swal.fire({
            icon: 'error',
            title: 'Error',
            text: 'No se pudo guardar el repuesto'
          });
        }
      });
    }
  }

  resetForm() {
    this.repuestoForm.reset();
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
      costoUnitario: repuesto.costoUnitario,
      stockMinimo: repuesto.stockMinimo,
      empresaId: repuesto.empresa.id,
      activo: repuesto.activo
    });

    if (this.authService.isAdmin() || this.authService.isAdminEmpresa()){
      this.mostrarNuevo = true;
    }  
  }

  confirmarEliminar(id: number) {
    //this.eliminar(id);
  }

}
