import { FormGroup } from '@angular/forms';

export class FormUtils {

  static getErrores(form: FormGroup): any {
    const errores: any = {};

    Object.keys(form.controls).forEach(campo => {
      const control = form.get(campo);

      if (control && control.errors) {
        errores[campo] = control.errors;
      }
    });

    return errores;
  }

  static getCamposInvalidos(form: FormGroup): string[] {
    return Object.keys(form.controls)
      .filter(campo => form.get(campo)?.invalid);
  }

  static getPrimerCampoInvalido(form: FormGroup): string | null {
    for (let campo of Object.keys(form.controls)) {
      if (form.get(campo)?.invalid) {
        return campo;
      }
    }
    return null;
  }

  static marcarComoTocados(form: FormGroup) {
    form.markAllAsTouched();
  }

  static esValido(form: FormGroup): boolean {
    return form.valid;
  }

  static formatearFechaLocal(fecha: Date): string {
    return fecha.getFullYear() + '-' +
      (fecha.getMonth() + 1).toString().padStart(2, '0') + '-' +
      fecha.getDate().toString().padStart(2, '0') + 'T' +
      fecha.getHours().toString().padStart(2, '0') + ':' +
      fecha.getMinutes().toString().padStart(2, '0');
  }
}