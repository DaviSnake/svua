import { Routes } from '@angular/router';
import { LoginComponent } from './auth/login/login.component';
import { authGuard } from './guards/auth.guard';
import { roleGuard } from './guards/role.guard';
import { escanearAccesoGuard } from './guards/escanear-acceso.guard';
import { controlTurnoAccesoGuard } from './guards/control-turno-acceso.guard';
import { informeMantencionesAccesoGuard } from './guards/informe-mantenciones-acceso.guard';
import { CalendarioComponent } from './calendar/calendario/calendario.component';
import { LayoutComponent } from './layout/layout.component';
import { ActivoComponent } from './components/activo/activo.component';
import { DashboardComponent } from './components/dashboard/dashboard.component';
import { EmpresaComponent } from './components/empresa/empresa.component';
import { DispositivoEmpresaComponent } from './components/dispositivo-empresa/dispositivo-empresa.component';
import { UsuarioComponent } from './components/usuario/usuario.component';
import { UbicacionComponent } from './components/ubicacion/ubicacion.component';
import { TipoActivoComponent } from './components/tipo-activo/tipo-activo.component';
import { ProveedorComponent } from './components/proveedor/proveedor.component';
import { BodegaComponent } from './components/bodega/bodega.component';
import { RepuestoComponent } from './components/repuesto/repuesto.component';
import { CargaMasivaComponent } from './components/carga-masiva/carga-masiva.component';
import { PerfilUsuarioComponent } from './components/perfil-usuario/perfil-usuario.component';
import { ConfiguracionComponent } from './components/configuracion/configuracion.component';
import { ForgotPasswordComponent } from './auth/forgot-password/forgot-password.component';
import { ResetPasswordComponent } from './components/reset-password/reset-password.component';
import { ReportesComponent } from './components/reportes/reportes.component';
import { AuditoriasComponent } from './components/auditorias/auditorias.component';
import { NotificacionComponent } from './components/notificacion/notificacion.component';
import { SoporteComponent } from './components/soporte/soporte.component';
import { SesionUsuarioComponent } from './components/sesion-usuario/sesion-usuario.component';
import { InformeConexionesComponent } from './components/informe-conexiones/informe-conexiones.component';
import { VerLogsComponent } from './components/ver-logs/ver-logs.component';
import { InformeMantencionesComponent } from './components/informe-mantenciones/informe-mantenciones.component';
import { EscanearComponent } from './components/escanear/escanear.component';
import { ControlTurnoComponent } from './components/control-turno/control-turno.component';
import { DepreciacionAceleradaComponent } from './components/depreciacion-acelerada/depreciacion-acelerada.component';

// 🔐 Roles que SI ven cada seccion "de gestion" segun el sidebar hoy
// (sidebar.component.html: *ngIf="!esTecnico" en esos items -- TECNICO es
// el unico rol que el sidebar oculta de estas secciones; el resto las ve
// todas igual, sin distincion entre ADMIN_EMPRESA/JEFE_MANTENIMIENTO/
// BODEGUERO/USUARIO).
const TODOS_MENOS_TECNICO = ['SUPER_ADMIN', 'ADMIN_EMPRESA', 'JEFE_MANTENIMIENTO', 'BODEGUERO', 'USUARIO'];

// 🔐 Control de Turno: a diferencia de TODOS_MENOS_TECNICO, aqui SI
// entra TECNICO (es quien registra las lecturas en terreno) y quedan
// fuera BODEGUERO/USUARIO (no participan de este proceso). Ya no se usa
// aca como data.roles -- se movio a control-turno-acceso.guard.ts
// (ROLES_CONTROL_TURNO local a ese archivo) porque tambien necesita
// cruzarse con Empresa.controlTurnoHabilitado, algo que data.roles no
// puede expresar.

export const routes: Routes = [
    {
        path:'', redirectTo:'/login', pathMatch: 'full'
    },
    { 
        path: 'login', component: LoginComponent 
    },
    { 
        path: 'forgot-password', component: ForgotPasswordComponent 
    },
    { 
        path: 'reset-password', component: ResetPasswordComponent 
    },
    {
        // 🔥 Antes: 21 objetos de ruta separados, todos con path: 'inicio'.
        // Angular's RouteReuseStrategy solo reutiliza el componente si
        // future.routeConfig === curr.routeConfig (misma referencia de
        // objeto): con 21 objetos distintos, cada navegación entre
        // secciones bajo /inicio destruía y recreaba LayoutComponent y
        // SidebarComponent (con reconexión de WebSocket incluida), aunque
        // el usuario nunca "salía" de /inicio. Se consolidan en un único
        // padre con 21 hijos, preservando exactamente el mismo componente,
        // guard y componentes hijos.
        path: 'inicio',
        component: LayoutComponent,
        canActivate: [authGuard], // 🔥 aquí
        // 🔐 roleGuard: replica a nivel de ruta las mismas restricciones
        // de rol que el sidebar ya aplica visualmente (*ngIf). Antes,
        // cualquier usuario logueado podia navegar directo por URL a una
        // seccion que el menu le oculta (defensa en profundidad).
        canActivateChild: [roleGuard],
        children: [
            {
                path: 'activo',
                component: ActivoComponent,
                data: { roles: TODOS_MENOS_TECNICO }
            },
            {
                path: 'dashboard',
                component: DashboardComponent,
                data: { roles: TODOS_MENOS_TECNICO }
            },
            {
                path: 'calendario',
                component: CalendarioComponent
                // Sin data.roles: visible para cualquier usuario logueado
                // (el sidebar tampoco lo restringe hoy).
            },
            {
                // ⚠️ Caso especial, igual que 'escanear': el sidebar usa
                // ROLES_CONTROL_TURNO Y Empresa.controlTurnoHabilitado (rol
                // Y flag de empresa) -- no se puede expresar con
                // data.roles, por eso un guard dedicado (ver
                // control-turno-acceso.guard.ts).
                path: 'controlTurno',
                component: ControlTurnoComponent,
                canActivate: [controlTurnoAccesoGuard]
            },
            {
                path: 'empresa',
                component: EmpresaComponent,
                data: { roles: ['SUPER_ADMIN'] }
            },
            {
                // 🔒 Que dispositivo fisico de monitoreo alimenta a que
                // empresa (ver CorreoLecturaImportador/DispositivoEmpresa).
                path: 'dispositivos',
                component: DispositivoEmpresaComponent,
                data: { roles: ['SUPER_ADMIN'] }
            },
            {
                path: 'ubicacion',
                component: UbicacionComponent,
                data: { roles: TODOS_MENOS_TECNICO }
            },
            {
                path: 'tipoActivo',
                component: TipoActivoComponent,
                data: { roles: TODOS_MENOS_TECNICO }
            },
            {
                path: 'proveedor',
                component: ProveedorComponent,
                data: { roles: TODOS_MENOS_TECNICO }
            },
            {
                path: 'bodega',
                component: BodegaComponent,
                data: { roles: TODOS_MENOS_TECNICO }
            },
            {
                path: 'repuesto',
                component: RepuestoComponent,
                data: { roles: TODOS_MENOS_TECNICO }
            },
            {
                // ⚠️ Sin data.roles: el sidebar hoy NO oculta "Usuarios" a
                // ningun rol (ni siquiera a TECNICO) -- se replica esa
                // misma laxitud tal cual, sin inventar una restricción
                // nueva. Vale la pena confirmar con negocio si esto es
                // intencional.
                path: 'usuario',
                component: UsuarioComponent
            },
            {
                path: 'reportes',
                component: ReportesComponent,
                data: { roles: TODOS_MENOS_TECNICO }
            },
            {
                path: 'auditorias',
                component: AuditoriasComponent,
                data: { roles: TODOS_MENOS_TECNICO }
            },
            {
                path: 'configuracion',
                component: ConfiguracionComponent,
                data: { roles: ['SUPER_ADMIN'] }
            },
            {
                path: 'perfilUsuario',
                component: PerfilUsuarioComponent
                // Sin data.roles: visible para cualquier usuario logueado.
            },
            {
                path: 'cargaMasiva',
                component: CargaMasivaComponent,
                data: { roles: TODOS_MENOS_TECNICO }
            },
            {
                path: 'notificaciones',
                component: NotificacionComponent,
                data: { roles: TODOS_MENOS_TECNICO }
            },
            {
                path: 'soporte',
                component: SoporteComponent
                // Sin data.roles: visible para cualquier usuario logueado.
            },
            {
                path: 'SesionesActivas',
                component: SesionUsuarioComponent,
                data: { roles: ['SUPER_ADMIN'] }
            },
            {
                path: 'informeConexiones',
                component: InformeConexionesComponent,
                data: { roles: ['SUPER_ADMIN'] }
            },
            {
                path: 'verLogs',
                component: VerLogsComponent,
                data: { roles: ['SUPER_ADMIN'] }
            },
            {
                // ⚠️ Caso especial: el sidebar usa ROL Y
                // Empresa.informeMantencionesHabilitado -- no se puede
                // expresar con data.roles, por eso un guard dedicado.
                path: 'informeMantenciones',
                component: InformeMantencionesComponent,
                canActivate: [informeMantencionesAccesoGuard]
            },
            {
                // ⚠️ Caso especial: el sidebar usa esAdmin || codigoQrHabilitado
                // || codigoEan13Habilitado (rol O flags de empresa) -- no se
                // puede expresar con data.roles, por eso un guard dedicado.
                path: 'escanear',
                component: EscanearComponent,
                canActivate: [escanearAccesoGuard]
            },
            {
                path: 'depreciacionAcelerada',
                component: DepreciacionAceleradaComponent,
                data: { roles: ['SUPER_ADMIN', 'ADMIN_EMPRESA'] }
            }
        ]
    },
    { path: '**', redirectTo: 'calendario' }
];
