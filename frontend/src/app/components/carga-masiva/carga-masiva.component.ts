import { Component, inject } from '@angular/core';
import { CargaMasivaService } from '../../services/carga-masiva.service';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-carga-masiva',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './carga-masiva.component.html',
  styleUrl: './carga-masiva.component.css'
})
export class CargaMasivaComponent {

  cargaMasivaService = inject(CargaMasivaService);

  fileNameActivo: string | null = null;
  fileNameProveedor: string | null = null;
  fileNameOrden: string | null = null;
  fileNameRepuesto: string | null = null;
  fileNameUbicacion: string | null = null;
  fileNameTipoActivo: string | null = null;
  progressActivo = 0;
  progressProveedor = 0;
  progressOrden = 0;
  progressRepuesto = 0;
  progressUbicacion = 0;
  progressTipoActivo = 0;
  estadoActivo = '';
  estadoProveedor = '';
  estadoOrden = '';
  estadoRepuesto = '';
  estadoUbicacion = '';
  estadoTipoActivo = '';
  interval: any;
  mensaje = '';
  mensajeActivo = '';
  mensajeProveedor = '';
  mensajeOrden = '';
  mensajeRepuesto = '';
  mensajeUbicacion = '';
  mensajeTipoActivo = '';

  onFileSelected(event: Event, archivo: String) {
    const input = event.target as HTMLInputElement;

    if (!input.files || input.files.length === 0) {
      return;
    }

    const file = input.files[0];
    
    if (archivo === "activo"){
      this.fileNameActivo = file.name;
      this.subir(file, archivo);
    } else if (archivo === "orden"){
      this.fileNameOrden = file.name;
      this.subir(file, archivo);
    } else if (archivo === "proveedor"){
      this.fileNameProveedor = file.name;
      this.subir(file, archivo);
    } else if (archivo === "repuesto"){
      this.fileNameRepuesto = file.name;
      this.subir(file, archivo);
    } else if (archivo === "ubicacion"){
      this.fileNameUbicacion = file.name;
      this.subir(file, archivo);
    } else if (archivo === "tipoActivo"){
      this.fileNameTipoActivo = file.name;
      this.subir(file, archivo);
    }

    input.value = '';
  }

  subir(file: File, archivo: String) {

    this.cargaMasivaService.upload(file, archivo).subscribe(res => {

      const jobId = res.jobId; // 👈 ahora viene como objeto JSON

      this.interval = setInterval(() => {

      this.cargaMasivaService.getProgress(jobId).subscribe(p => {

        console.log(p.estado); 

      if (p.estado === 'COMPLETADO' || p.estado === 'COMPLETADO_CON_ERRORES' || p.estado === 'ERROR') {
        clearInterval(this.interval);

        switch (p.estado) {
          case 'ERROR':
            this.mensaje = '❌ Error en el proceso de carga';
            break;

          case 'COMPLETADO_CON_ERRORES':
            this.mensaje = '❌ Proceso de carga terminado con errores';
            console.log('Errores:', p.erroresDetalle);
            break;
        }
      }
      
      if (archivo === "activo"){
        this.mensajeActivo = this.mensaje;
        this.estadoActivo = p.estado;
        this.progressActivo = Math.round((p.procesados / p.total) * 100);
      } else if (archivo === "proveedor"){
        this.mensajeProveedor = this.mensaje;
        this.estadoProveedor = p.estado;
        this.progressProveedor = Math.round((p.procesados / p.total) * 100);
      } else if (archivo === "orden"){
        this.mensajeOrden = this.mensaje;
        this.estadoOrden = p.estado;
        this.progressOrden = Math.round((p.procesados / p.total) * 100);
      } else if (archivo === "repuesto"){
        this.mensajeRepuesto = this.mensaje;
        this.estadoRepuesto = p.estado;
        this.progressRepuesto = Math.round((p.procesados / p.total) * 100);
      } else if (archivo === "ubicacion"){
        this.mensajeUbicacion = this.mensaje;
        this.estadoUbicacion = p.estado;
        this.progressUbicacion = Math.round((p.procesados / p.total) * 100);
      } else if (archivo === "tipoActivo"){
        this.mensajeTipoActivo = this.mensaje;
        this.estadoTipoActivo = p.estado;
        this.progressTipoActivo = Math.round((p.procesados / p.total) * 100);
      }

      

    });

    }, 1000);

    });
  }

  reset(archivo: String) {

    if (archivo === "activo"){
      this.fileNameActivo = null;
      this.progressActivo = 0;
      this.mensajeActivo = '';
    } else if (archivo === "proveedor"){
      this.fileNameProveedor = null;
      this.progressProveedor = 0;
      this.mensajeProveedor = '';
    } else if (archivo === "orden"){
      this.fileNameOrden = null;
      this.progressOrden = 0;
      this.mensajeOrden = '';
    } else if (archivo === "repuesto"){
      this.fileNameRepuesto = null;
      this.progressRepuesto = 0;
      this.mensajeRepuesto = '';
    } else if (archivo === "ubicacion"){
      this.fileNameUbicacion = null;
      this.progressUbicacion = 0;
      this.mensajeUbicacion = '';
    } else if (archivo === "tipoActivo"){
      this.fileNameTipoActivo = null;
      this.progressTipoActivo = 0;
      this.mensajeTipoActivo = '';
    }
    this.mensaje = '';

  }

}
