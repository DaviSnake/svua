import { Component } from '@angular/core';
import { PaginaencontruccionComponent } from "../paginaencontruccion/paginaencontruccion.component";

@Component({
  selector: 'app-reportes',
  standalone: true,
  imports: [PaginaencontruccionComponent],
  templateUrl: './reportes.component.html',
  styleUrl: './reportes.component.css'
})
export class ReportesComponent {

}
