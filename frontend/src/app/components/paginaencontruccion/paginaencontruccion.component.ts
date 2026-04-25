import { Component, OnInit } from '@angular/core';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-paginaencontruccion',
  standalone: true,
  imports: [RouterModule],
  templateUrl: './paginaencontruccion.component.html',
  styleUrl: './paginaencontruccion.component.css'
})
export class PaginaencontruccionComponent  implements OnInit {
  
  ngOnInit(): void {
    localStorage.clear();
  }

}
