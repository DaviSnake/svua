// Declaraciones ambient minimas para librerias sin tipos propios instalados
// en este proyecto (qrcode, jsbarcode y zxing). Se usan para dibujar/leer,
// en el modal "Ver" de Activo y en la pantalla de Escaneo, el QR y el
// codigo de barras EAN13 a partir del texto ya generado por el backend
// (ver ActivoCodigoGenerador).
declare module 'qrcode';
declare module 'jsbarcode';
declare module '@zxing/library';
