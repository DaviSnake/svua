import { Component, inject, OnInit } from '@angular/core';
import { Empresa } from '../../model/empresa';
import { Activo } from '../../model/activo';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { EmpresaService } from '../../services/empresa.service';
import { ActivoService } from '../../services/activo.service';
import { CommonModule } from '@angular/common';
import { FormUtils } from '../../shared/form-utils';
import Swal from 'sweetalert2';
import jsPDF from 'jspdf';
import * as QRCode from 'qrcode';
import JsBarcode from 'jsbarcode';

@Component({
  selector: 'app-empresa',
  standalone: true,
  imports: [FormsModule, ReactiveFormsModule, CommonModule],
  templateUrl: './empresa.component.html',
  styleUrl: './empresa.component.css'
})
export class EmpresaComponent implements OnInit {

  empresaService = inject(EmpresaService);
  activoService = inject(ActivoService);
  fb = inject(FormBuilder);

  // 🖨️ PDF imprimible de codigos QR/EAN13 por empresa (solo si la empresa
  // tiene alguno de los dos habilitado). Ver generarPdfCodigos().
  generandoPdfId: number | null = null;

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
      telefono: ['', [Validators.required, Validators.pattern('^[+0-9]+$')]],
      direccion: ['', Validators.required],
      tipoPlan: ['FREE', Validators.required],

      // 🔹 Configuracion (demo + codigos QR/EAN13 de activos)
      demo: [false],
      codigoQrHabilitado: [false],
      codigoEan13Habilitado: [false],

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

      return;
    }

    const empresa: Empresa = this.empresaForm.value;
    
    this.flag = this.mostrarAdmin ? 1 : 0;

    this.loading = true;

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
              this.mostrarAdmin = false
              this.cargarEmpresas(); // 🔄 refrescar tabla
              this.resetForm();
            },
            error: (err) => {
              console.log(err.error); // 👈 DEBUG
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
    this.flag = 0;
    this.editando = true;
    this.empresaId = emp.id!;
    this.empresaEditandoId = emp.id!
    this.desactivarSeccionAdmin(); // 🐛 FIX: ver comentario en el metodo
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
    this.empresaForm.reset({
      tipoPlan: 'FREE',
      demo: false,
      codigoQrHabilitado: false,
      codigoEan13Habilitado: false
    });
    this.editando = false;
    this.loading = false;
    this.empresaEditandoId = null;
    this.flag = 0;
    this.desactivarSeccionAdmin(); // 🐛 FIX: ver comentario en el metodo
  }

  togglePassword() {
    this.showPassword = !this.showPassword;
  }

  // 🐛 FIX: apaga la seccion "Administrador" y limpia sus validators de
  // forma DETERMINISTICA (a diferencia de onAdminClick(), que es un
  // toggle). Antes, resetForm() llamaba a onAdminClick() para "cerrar"
  // la seccion admin, pero como es un toggle podia terminar dejandola
  // ABIERTA (mostrarAdmin = true) y con adminNombre/adminEmail/
  // adminPassword marcados como required - validators que sobrevivian a
  // pesar de que esa seccion esta oculta al editar (*ngIf="!editando"),
  // haciendo fallar la validacion del formulario de EDITAR con "Revisa
  // el campo: adminNombre" aunque el usuario nunca haya tocado esos
  // campos.
  private desactivarSeccionAdmin(): void {

    this.mostrarAdmin = false;

    const adminNombre = this.empresaForm.get('adminNombre');
    const adminEmail = this.empresaForm.get('adminEmail');
    const adminPassword = this.empresaForm.get('adminPassword');

    adminNombre?.clearValidators();
    adminEmail?.clearValidators();
    adminPassword?.clearValidators();

    adminNombre?.updateValueAndValidity();
    adminEmail?.updateValueAndValidity();
    adminPassword?.updateValueAndValidity();
  }

  onAdminClick() {

    const adminNombre = this.empresaForm.get('adminNombre');
    const adminEmail = this.empresaForm.get('adminEmail');
    const adminPassword = this.empresaForm.get('adminPassword');
    this.mostrarAdmin = !this.mostrarAdmin;
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

  }

  // 🔥 trackBy para la tabla principal de empresas.
  trackByEmpresaId(index: number, empresa: any): any {
    return empresa?.id ?? index;
  }

  /*
   * =========================================
   * PDF IMPRIMIBLE DE CODIGOS QR / EAN13
   * =========================================
   */

  // 🖨️ Genera un PDF con el QR y/o el codigo de barras EAN13 de cada
  // activo de la empresa, listo para imprimir y pegar en el activo
  // fisico. Solo incluye los codigos que la EMPRESA (no el usuario que
  // genera el PDF) tenga habilitados: si es SUPER_ADMIN quien lo genera,
  // el activo puede traer ambos codigos igual (SUPER_ADMIN los ve
  // siempre), por eso el filtro se hace acá según la configuración de
  // la empresa elegida, no según lo que el activo trae en la respuesta.
  generarPdfCodigos(empresa: Empresa): void {

    if (!empresa.id || this.generandoPdfId) {
      return;
    }

    const incluyeQr = !!empresa.codigoQrHabilitado;
    const incluyeEan = !!empresa.codigoEan13Habilitado;

    if (!incluyeQr && !incluyeEan) {
      Swal.fire({
        icon: 'info',
        title: 'Sin códigos habilitados',
        text: 'Esta empresa no tiene código QR ni EAN13 habilitado.'
      });
      return;
    }

    this.generandoPdfId = empresa.id;

    // 🔥 size grande: cubre incluso el plan ENTERPRISE (max 1000 activos).
    // Si en el futuro se permiten mas, esto tendria que paginar.
    this.activoService.getActivoCombo(0, 1000, empresa.id).subscribe({
      next: (pagina) => {

        const activos = (pagina.content ?? []).filter(a =>
          (incluyeQr && a.codigoQr) || (incluyeEan && a.codigoEan13)
        );

        if (!activos.length) {
          this.generandoPdfId = null;
          Swal.fire({
            icon: 'info',
            title: 'Sin activos',
            text: 'Esta empresa no tiene activos con códigos generados todavía.'
          });
          return;
        }

        this.construirPdfCodigos(empresa, activos, incluyeQr, incluyeEan)
          .catch(() => {
            Swal.fire({
              icon: 'error',
              title: 'Error',
              text: 'No se pudo generar el PDF de códigos'
            });
          })
          .finally(() => {
            this.generandoPdfId = null;
          });
      },
      error: () => {
        this.generandoPdfId = null;
        Swal.fire({
          icon: 'error',
          title: 'Error',
          text: 'No se pudieron obtener los activos de la empresa'
        });
      }
    });
  }

  // 🔳 Arma el PDF: una tarjeta por activo (2 columnas), con su nombre,
  // codigo interno, y el/los codigo(s) visual(es) que correspondan. Cada
  // tarjeta queda pensada para imprimir y recortar/pegar en el activo.
  private async construirPdfCodigos(
    empresa: Empresa,
    activos: Activo[],
    incluyeQr: boolean,
    incluyeEan: boolean
  ): Promise<void> {

    const doc = new jsPDF({ unit: 'mm', format: 'a4' });

    const margenX = 10;
    const margenY = 18;
    const columnas = 2;
    const anchoTarjeta = 90;
    const altoTarjeta = 55;
    const espacioX = 5;
    const espacioY = 5;
    const altoPagina = 297;
    const filasPorPagina = Math.floor((altoPagina - margenY * 2) / (altoTarjeta + espacioY));

    const dibujarEncabezado = () => {
      doc.setFontSize(13);
      doc.setFont('helvetica', 'bold');
      doc.text(`Códigos de Activos - ${empresa.nombre}`, margenX, 10);
      doc.setFont('helvetica', 'normal');
    };

    dibujarEncabezado();

    let col = 0;
    let fila = 0;

    for (const activo of activos) {

      if (fila >= filasPorPagina) {
        doc.addPage();
        fila = 0;
        col = 0;
        dibujarEncabezado();
      }

      const x = margenX + col * (anchoTarjeta + espacioX);
      const y = margenY + fila * (altoTarjeta + espacioY);

      doc.setDrawColor(200);
      doc.rect(x, y, anchoTarjeta, altoTarjeta);

      doc.setFontSize(10);
      doc.setFont('helvetica', 'bold');
      doc.text(activo.nombre ?? '', x + 3, y + 6, { maxWidth: anchoTarjeta - 6 });
      doc.setFont('helvetica', 'normal');
      doc.setFontSize(9);
      doc.text(`Código: ${activo.codigoInterno ?? ''}`, x + 3, y + 11);

      const offsetY = y + 15;

      if (incluyeQr && activo.codigoQr) {
        const qrDataUrl = await QRCode.toDataURL(activo.codigoQr, { width: 200, margin: 1 });
        doc.addImage(qrDataUrl, 'PNG', x + 3, offsetY, 32, 32);
      }

      if (incluyeEan && activo.codigoEan13) {
        const barcodeDataUrl = this.generarBarcodeDataUrl(activo.codigoEan13);
        const barX = incluyeQr ? x + 38 : x + 3;
        const barAncho = incluyeQr ? anchoTarjeta - 41 : anchoTarjeta - 6;
        doc.addImage(barcodeDataUrl, 'PNG', barX, offsetY + 6, barAncho, 20);
      }

      col++;
      if (col >= columnas) {
        col = 0;
        fila++;
      }
    }

    doc.save(`codigos-${this.slug(empresa.nombre)}.pdf`);
  }

  // 🔳 JsBarcode puede dibujar directo sobre un <canvas> (sin pasar por
  // SVG), asi que se usa un canvas descartable, fuera del DOM, solo para
  // obtener el PNG que despues se inserta en el PDF.
  private generarBarcodeDataUrl(valor: string): string {
    const canvas = document.createElement('canvas');
    JsBarcode(canvas, valor, {
      format: 'EAN13',
      width: 2,
      height: 60,
      displayValue: true,
      fontSize: 12
    });
    return canvas.toDataURL('image/png');
  }

  private slug(texto: string | undefined): string {
    return (texto || 'empresa')
      .toLowerCase()
      .normalize('NFD').replace(/[\u0300-\u036f]/g, '')
      .replace(/[^a-z0-9]+/g, '-')
      .replace(/(^-|-$)/g, '');
  }

}
