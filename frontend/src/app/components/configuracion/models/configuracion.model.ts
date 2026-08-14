// 🔒 Refleja el .env real que usa docker-compose (ver ConfiguracionServiceImpl
// en el backend). Cada entrada es una variable del archivo (clave=valor).
export interface ConfiguracionEntry {
  clave: string;
  valor: string;
}
