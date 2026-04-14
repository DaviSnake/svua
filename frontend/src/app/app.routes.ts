import { Routes } from '@angular/router';
import { LoginComponent } from './auth/login/login.component';
import { authGuard } from './guards/auth.guard';
import { CalendarioComponent } from './calendar/calendario/calendario.component';
import { LayoutComponent } from './layout/layout.component';
import { ActivoComponent } from './components/activo/activo.component';
import { DashboardComponent } from './components/dashboard/dashboard.component';
import { EmpresaComponent } from './components/empresa/empresa.component';
import { UsuarioComponent } from './components/usuario/usuario.component';
import { UbicacionComponent } from './components/ubicacion/ubicacion.component';
import { TipoActivoComponent } from './components/tipo-activo/tipo-activo.component';
import { ProveedorComponent } from './components/proveedor/proveedor.component';
import { BodegaComponent } from './components/bodega/bodega.component';
import { RepuestoComponent } from './components/repuesto/repuesto.component';

export const routes: Routes = [
    {
        path:'', redirectTo:'/login', pathMatch: 'full'
    },
    { 
        path: 'login', component: LoginComponent 
    },
    {
        path: 'inicio',
        component: LayoutComponent,
        canActivate: [authGuard], // 🔥 aquí
        children: [
            {
                path: 'activo',
                component: ActivoComponent
            }
        ]
    },
    {
        path: 'inicio',
        component: LayoutComponent,
        canActivate: [authGuard], // 🔥 aquí
        children: [
            {
                path: 'dashboard',
                component: DashboardComponent
            }
        ]
    },
    {
        path: 'inicio',
        component: LayoutComponent,
        canActivate: [authGuard], // 🔥 aquí
        children: [
            {
                path: 'calendario',
                component: CalendarioComponent
            }
        ]
    },
    {
        path: 'inicio',
        component: LayoutComponent,
        canActivate: [authGuard], // 🔥 aquí
        children: [
            {
                path: 'empresa',
                component: EmpresaComponent
            }
        ]
    },
    {
        path: 'inicio',
        component: LayoutComponent,
        canActivate: [authGuard], // 🔥 aquí
        children: [
            {
                path: 'ubicacion',
                component: UbicacionComponent
            }
        ]
    },
    {
        path: 'inicio',
        component: LayoutComponent,
        canActivate: [authGuard], // 🔥 aquí
        children: [
            {
                path: 'tipoActivo',
                component: TipoActivoComponent
            }
        ]
    },
    {
        path: 'inicio',
        component: LayoutComponent,
        canActivate: [authGuard], // 🔥 aquí
        children: [
            {
                path: 'proveedor',
                component: ProveedorComponent
            }
        ]
    },
    {
        path: 'inicio',
        component: LayoutComponent,
        canActivate: [authGuard], // 🔥 aquí
        children: [
            {
                path: 'bodega',
                component: BodegaComponent
            }
        ]
    },
    {
        path: 'inicio',
        component: LayoutComponent,
        canActivate: [authGuard], // 🔥 aquí
        children: [
            {
                path: 'repuesto',
                component: RepuestoComponent
            }
        ]
    },
    {
        path: 'inicio',
        component: LayoutComponent,
        canActivate: [authGuard], // 🔥 aquí
        children: [
            {
                path: 'usuario',
                component: UsuarioComponent
            }
        ]
    },
    { path: '**', redirectTo: 'calendario' }
];
