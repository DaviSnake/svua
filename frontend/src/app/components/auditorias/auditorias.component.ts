import { Component } from '@angular/core';
import { PaginaencontruccionComponent } from "../paginaencontruccion/paginaencontruccion.component";

@Component({
  selector: 'app-auditorias',
  standalone: true,
  imports: [PaginaencontruccionComponent],
  templateUrl: './auditorias.component.html',
  styleUrl: './auditorias.component.css'
})
export class AuditoriasComponent {

}
