# Townsquare

Mod social server-side para servidores Fabric, Minecraft 1.21.11. Modid: `townsquare`.
Tablones comunitarios, correo entre jugadores y gremios. **Los jugadores no
instalan nada**: `"environment": "*"` para poder cargarlo tambien en solitario; la garantia de
cero instalacion es no tener contenido cliente, no ese flag.

## Decisiones fundacionales

- **Sin bloques ni items propios.** Necesitarian texturas en el cliente y romperian
  "cero instalacion". Un tablon es un atril de vanilla marcado por un admin; las
  notas son libros escritos de vanilla; la interfaz son los menus de vanilla.
- **Sin backend.** Todo vive en el mundo del servidor. Nada sale de el.
- **Sin dependencias mas alla de Fabric API.**
- El nicho es pequeno (miles de descargas, no millones) y se sabe. El objetivo es
  un mod util y terminado para administradores de servidor.

## Mappings

Oficiales de Mojang (`loom.officialMojangMappings()`), NO Yarn. Los tutoriales
suelen estar en Yarn y los nombres no coinciden. En 1.21.11 `ResourceLocation` se
llama `Identifier`, `hasPermission(int)` no existe (es `PermissionSet`), y
`GameProfile` es un record con `name()`. Verifica siempre con `javap` sobre los
jars de `.gradle/loom-cache/` antes de usar una API nueva.

## Reglas no negociables

1. No escribas APIs de Minecraft de memoria. Verifica la firma real con `javap`.
2. `./gradlew build` despues de cada sistema. No acumules.
3. La logica sin Minecraft va en paquetes puros y se testea sin arrancar el juego.
   Lo que toca Minecraft, en una capa fina aparte. Mantener esa frontera.
4. Persistencia con codecs, nunca NBT a mano, y siempre con version de esquema.
5. IDs en ingles snake_case. Textos al jugador en INGLES y como texto plano:
   las claves de `lang/` las resuelve el cliente, y nuestros jugadores no tienen
   el mod, asi que verian la clave cruda. Excepcion consciente a la practica
   habitual, valida solo mientras el mod sea 100% server-side.
6. Desconectarse nunca puede mejorar la posicion del jugador.

## Estado actual

El tablon funciona: comando de admin sobre un atril, cofre de vanilla al usarlo,
contenido persistido por dimension via attachment con codec. Verificado en
servidor dedicado; falta probarlo con jugador conectado.

El correo funciona: /mail send <jugador> <texto>, /mail sendbook (adjunta el item
de la mano), aviso al conectar y entrega con /mail read. Buzones por nombre en
minusculas en un attachment del overworld: funciona con el destinatario offline.

Los gremios funcionan: create/invite/join/leave/info y chat con /g. El fundador
no abandona: disuelve. Pertenencia derivada escaneando, sin indice aparte.
El tablon ya filtra: solo retiene libros, lo demas se devuelve al cerrar.

Pulido hecho: mensajes en ingles, README, LICENSE MIT, version 1.0.0.
Antes de publicar: probar en dedicado con 2 cuentas (invite/join/mail entre
dos jugadores) y hacer un icono 128x128.
se abre un contenedor con las notas (libros escritos); clavar y quitar notas.

## Comandos utiles

    ./gradlew build       # compilar y tests
    ./gradlew runServer   # servidor dedicado de pruebas (aceptar el EULA en run/)
