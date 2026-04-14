import { Component, inject, OnInit } from '@angular/core';
import { Empresa } from '../../model/empresa';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { EmpresaService } from '../../services/empresa.service';
import { CommonModule } from '@angular/common';
import { FormUtils } from '../../shared/form-utils';
import Swal from 'sweetalert2';

@Component({
  selector: 'app-empresa',
  standalone: true,
  imports: [FormsModule, ReactiveFormsModule, CommonModule],
  templateUrl: './empresa.component.html',
  styleUrl: './empresa.component.css'
})
export class EmpresaComponent implements OnInit {

  empresaService = inject(EmpresaService);
  fb = inject(FormBuilder);

  empresas: Empresa[] = [];
  empresasFiltradas: Empresa[] = [];

  showPassword = false;

  empresaForm!: FormGroup;
  editando = false;
  empresaId!: number;
  flag!: number;

  empresaEditandoId: number | null = null;
  empresaSeleccionado: any = null;

  filtro = '';
  mensaje = '';
  loading = false;
  mostrarAdmin = false;
  
  page = 0;
  size = 10;

  totalPages = 0;
  totalElements = 0;


  

  ngOnInit(): void {
    this.initForm();
    this.cargarEmpresas();
  }

  initForm() {
    this.empresaForm = this.fb.group({
      nombre: ['', Validators.required],
      rut: ['', Validators.required],
      //rut: ['', [Validators.required, rutValidator]],
      emailContacto: ['', [Validators.required, Validators.email]],
      telefono: ['', [Validators.required, Validators.pattern('^[0-9]+$')]],
      direccion: ['', Validators.required],
      tipoPlan: ['FREE', Validators.required],

      // ADMIN
      adminNombre: [''],
      adminEmail: [''],
      adminPassword: ['']
    });
  }

  

  cargarEmpresas() {
    this.empresaService.getAll().subscribe(data => {
      this.empresas = data;
      this.empresasFiltradas = data;
    });
  }

  guardar() {
    if (!FormUtils.esValido(this.empresaForm)) {
      const campo = FormUtils.getPrimerCampoInvalido(this.empresaForm);
      FormUtils.marcarComoTocados(this.empresaForm);
      Swal.fire({
        icon: 'warning',
        title: 'Formulario incompleto',
        text: `Revisa el campo: ${campo}`
      });

      console.log(FormUtils.getErrores(this.empresaForm));

      return;
    }

    const empresa: Empresa = this.empresaForm.value;
    const adminNombre = this.empresaForm.get('adminNombre');
    const adminEmail = this.empresaForm.get('adminEmail');
    const adminPassword = this.empresaForm.get('adminPassword');

    if (this.mostrarAdmin) {
      this.flag = 1;
      adminNombre?.setValidators([Validators.required]);
      adminEmail?.setValidators([Validators.required, Validators.email]);
      adminPassword?.setValidators([Validators.required]);
    } else {
      adminNombre?.clearValidators();
      adminEmail?.clearValidators();
      adminPassword?.clearValidators();
    }

    adminNombre?.updateValueAndValidity();
    adminEmail?.updateValueAndValidity();
    adminPassword?.updateValueAndValidity();

    this.loading = true;
    console.log(empresa);

    if (this.editando && this.empresaEditandoId !== null) {
      // EDITAR
      Swal.fire({
        title: '¿Estás seguro?',
        text: 'Esta acción actualizará la empresa',
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

          this.empresaService.update(this.empresaEditandoId!, empresa).subscribe({
            next: () => {
              Swal.fire({
                icon: 'success',
                title: 'Actualizado',
                text: 'La empresa fue actualizado correctamente',
                timer: 2000,
                showConfirmButton: false
              });

              this.cargarEmpresas(); // 🔄 refrescar tabla
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
      this.empresaService.create(empresa, this.flag).subscribe({
        next: () => {
          this.resetForm();
          this.cargarEmpresas();

          Swal.fire({
            icon: 'success',
            title: '¡Guardado!',
            text: 'La empresa fue creado correctamente',
            confirmButtonColor: '#3498db'
          });
        },
        error: () => {
          Swal.fire({
            icon: 'error',
            title: 'Error',
            text: 'No se pudo guardar la empresa'
          });
        }
      });
    }
  }

  editar(emp: Empresa) {
    this.editando = true;
    this.empresaId = emp.id!;
    this.empresaForm.patchValue(emp);
  }

  eliminar(id: number) {
    if (!confirm('¿Eliminar empresa?')) return;

    this.empresaService.delete(id).subscribe(() => {
      this.cargarEmpresas();
    });
  }

  filtrar() {
    this.empresasFiltradas = this.empresas.filter(e =>
      e.nombre.toLowerCase().includes(this.filtro.toLowerCase()) ||
      e.rut.toLowerCase().includes(this.filtro.toLowerCase()) ||
      e.emailContacto.toLowerCase().includes(this.filtro.toLowerCase())
    );
  }

  resetForm() {
    this.empresaForm.reset({ tipoPlan: 'FREE' });
    this.editando = false;
    this.loading = false;
    this.empresaEditandoId = null;
  }

  togglePassword() {
    this.showPassword = !this.showPassword;
  }

}
