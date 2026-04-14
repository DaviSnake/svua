// rut.validator.ts
import { AbstractControl, ValidationErrors } from '@angular/forms';

export function rutValidator(control: AbstractControl): ValidationErrors | null {
  if (!control.value) return null;

  let rut = control.value.replace(/\./g, '').replace('-', '');

  if (rut.length < 2) return { rutInvalido: true };

  const cuerpo = rut.slice(0, -1);
  let dv = rut.slice(-1).toUpperCase();

  let suma = 0;
  let multiplo = 2;

  for (let i = cuerpo.length - 1; i >= 0; i--) {
    suma += Number(cuerpo[i]) * multiplo;
    multiplo = multiplo === 7 ? 2 : multiplo + 1;
  }

  const dvEsperado = 11 - (suma % 11);

  let dvFinal = '';
  if (dvEsperado === 11) dvFinal = '0';
  else if (dvEsperado === 10) dvFinal = 'K';
  else dvFinal = dvEsperado.toString();

  return dv === dvFinal ? null : { rutInvalido: true };
}