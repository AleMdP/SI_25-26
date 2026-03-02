globals [
  tur-totales                ;; Monitor: Total de vehículos generados
  cuenta-entrados            ;; Monitor: Vehículos que cruzan la ZBE
  cuenta-desviados-barrera   ;; Monitor: Vehículos rechazados en la barrera
  prob-combustion            ;; Porcentaje actual de coches de combustión
  prob-electrico             ;; Porcentaje actual de coches eléctricos
]

patches-own [
  es-zbe?                    ;; Identifica si el parche es parte de la zona restringida
  contaminacion              ;; Nivel de CO2 acumulado en el parche
]

turtles-own [
  tipo-motor                 ;; "electrico", "hibrido" o "combustion"
  consumo-litros             ;; Consumo simulado para calcular emisiones
  generacion-CO2             ;; g/km de CO2
  desvio-izq? desvio-der?    ;; Dirección del desvío
  en-maniobra?               ;; Estado de giro activo
  es-coche-central?          ;; Identifica si el coche nació en carril con ZBE
  ya-contabilizado?          ;; Evita contar varias veces al mismo coche en la barrera
]

to setup
  clear-all
  set tur-totales 0
  set cuenta-entrados 0
  set cuenta-desviados-barrera 0

  ;; Valores iniciales según DGT 2024 (ajustados para visibilidad inicial)
  set prob-combustion 85
  set prob-electrico 5

  ask patches [
    set pcolor gray
    set es-zbe? false
    set contaminacion 0

    ;; 1. Dibujar Zona Verde (ZBE)
    if abs pxcor <= 7 and abs pycor <= 7 [ set pcolor green ]

    ;; 2. Dibujar Carriles Verticales (Carreteras)
    if member? pxcor [-12 -3 3 12] [
      set pcolor white
      if (abs pxcor <= 7) and (abs pycor <= 7) [ set es-zbe? true ]
    ]

    ;; 3. Dibujar Carriles de Desvío/Retorno
    if (pycor = -8 or pycor = 8) and ((pxcor > -12 and pxcor < -3) or (pxcor > 3 and pxcor < 12)) [
      set pcolor white
      set es-zbe? false
    ]
  ]
  reset-ticks
end

to go
  ;; --- EVOLUCIÓN DEL PARQUE MÓVIL ---
  evolucionar-parque-movil

  ;; --- GENERACIÓN DE TRÁFICO ---
  if generar-trafico? = true and intensidad-trafico > 0 [
    let frecuencia (11 - intensidad-trafico)

    if ticks mod frecuencia = 0 [
      foreach [-12 -3 3 12] [ c ->
        ask patch c -16 [
          if not any? turtles-here [
            sprout 1 [
              set tur-totales tur-totales + 1
              set shape "car" set size 1.5 set heading 0
              set en-maniobra? false set desvio-izq? false set desvio-der? false
              set ya-contabilizado? false
              set es-coche-central? (abs pxcor = 3)

              asignar-tipo
              asignar-consumo
              calculo-co2-100

              if tipo-motor = "electrico" [ set color blue ]
              if tipo-motor = "hibrido"   [ set color green ]
              if tipo-motor = "combustion" [ set color red ]
            ]
          ]
        ]
      ]
    ]
  ]

  ask turtles [
    contaminar
    mover-coche
  ]

  if ticks mod 3 = 0 [ limpiar-aire ]
  actualizar-visualizacion-contaminacion
  tick
end

to evolucionar-parque-movil
  ;; Cada 1000 ticks simulamos un avance tecnológico/renovación
  if ticks > 0 and ticks mod 1000 = 0 [
    if prob-combustion > 0 [
      set prob-combustion (prob-combustion - 5)
      set prob-electrico (prob-electrico + 5)

      ;; Mostramos el progreso en la consola de comandos
      print (word "--- RENOVACIÓN DEL PARQUE (Tick " ticks ") ---")
      print (word "Prob. Combustión: " prob-combustion "% | Prob. Eléctricos: " prob-electrico "%")
    ]
  ]
end

to mover-coche
  ;; --- BARRERA ZBE ---
  if pycor = -8 and es-coche-central? and not ya-contabilizado? [
    ifelse [es-zbe?] of patch-at 0 1 and generacion-CO2 > limite-emisiones [
      set en-maniobra? true
      set cuenta-desviados-barrera cuenta-desviados-barrera + 1
      set ya-contabilizado? true
      ifelse pxcor < 0 [ set desvio-izq? true set heading -90 ]
                       [ set desvio-der? true set heading 90 ]
    ] [
      set cuenta-entrados cuenta-entrados + 1
      set ya-contabilizado? true
    ]
  ]

  ;; --- MANIOBRAS PARA RETORNO ---
  if pycor = 8 and not en-maniobra? [
    if desvio-izq? [ set en-maniobra? true set heading 90 ]
    if desvio-der? [ set en-maniobra? true set heading -90 ]
  ]

  if en-maniobra? [
    if (pycor = -8 and abs pxcor = 12) [ set heading 0 set en-maniobra? false ]
    if (pycor = 8 and abs pxcor = 3) [
      set heading 0 set en-maniobra? false
      set desvio-izq? false set desvio-der? false
    ]
  ]

  ifelse not any? turtles-at 0 1 [ fd 1 ] [ ]

  if ycor >= max-pycor [ die ]
end

to contaminar
  if pcolor != green and pcolor != gray [
    set contaminacion contaminacion + (generacion-CO2 / 100)
  ]
end

to limpiar-aire
  ask patches [
    if pcolor = green or pcolor = gray [ set contaminacion 0 ]
    if contaminacion > 0 [
      set contaminacion (contaminacion - renovacion-aire)
      if contaminacion < 0 [ set contaminacion 0 ]
    ]
  ]
end

to actualizar-visualizacion-contaminacion
  ask patches [
    if pcolor != gray and pcolor != green [
      ifelse contaminacion > 0 [
        let n-color 19.9 - (contaminacion * 0.5)
        if n-color < 10 [ set n-color 10 ]
        set pcolor n-color
      ] [ set pcolor white ]
    ]
  ]
end

to asignar-tipo
  let r random-float 100

  ;; El rango de híbridos se mantiene estable en el 10% (de prob-electrico a prob-electrico + 10)
  ifelse r < prob-electrico [
    set tipo-motor "electrico"
  ] [
    ifelse r < (prob-electrico + 10) [
      set tipo-motor "hibrido"
    ] [
      set tipo-motor "combustion"
    ]
  ]
end

to asignar-consumo
  if tipo-motor = "electrico" [ set consumo-litros 0 ]
  if tipo-motor = "hibrido" [ set consumo-litros 3.5 + random-float 1.5 ]
  if tipo-motor = "combustion" [ set consumo-litros 5.5 + random-float 6.5 ]
end

to calculo-co2-100
  set generacion-CO2 (consumo-litros * 2300 / 100)
end
@#$#@#$#@
GRAPHICS-WINDOW
425
10
862
448
-1
-1
13.0
1
10
1
1
1
0
1
1
1
-16
16
-16
16
0
0
1
ticks
30.0

BUTTON
327
449
412
482
Comenzar
setup
NIL
1
T
OBSERVER
NIL
NIL
NIL
NIL
1

BUTTON
349
486
412
519
NIL
go
T
1
T
OBSERVER
NIL
NIL
NIL
NIL
1

SLIDER
166
49
338
82
limite-emisiones
limite-emisiones
0
250
92.0
1
1
NIL
HORIZONTAL

PLOT
11
296
211
446
Contaminación en ZBE
Tiempo (Ticks)
CO2 g/Km
0.0
10.0
0.0
10.0
true
false
"" ""
PENS
"default" 1.0 0 -2674135 true "" "plot sum [contaminacion] of patches with [es-zbe? = true and pcolor != green]"

PLOT
213
296
413
446
Contaminación fuera de ZBE
Tiempo (Ticks)
CO2 g/Km
0.0
10.0
0.0
10.0
true
false
"" ""
PENS
"default" 1.0 0 -8053223 true "" "plot sum [contaminacion] of patches with [es-zbe? = false and pcolor != gray]"

MONITOR
143
148
285
209
Turismos Totales
tur-totales
0
1
15

MONITOR
41
220
190
281
% Entran en ZBE
ifelse-value (cuenta-entrados + cuenta-desviados-barrera > 0) \n[ (cuenta-entrados / (cuenta-entrados + cuenta-desviados-barrera)) * 100 ] [ 0 ]
2
1
15

MONITOR
247
221
366
282
% Se desvían
ifelse-value (cuenta-entrados + cuenta-desviados-barrera > 0) \n[ (cuenta-desviados-barrera / (cuenta-entrados + cuenta-desviados-barrera)) * 100 ] [ 0 ]
2
1
15

SLIDER
165
13
337
46
intensidad-trafico
intensidad-trafico
1
10
2.0
1
1
NIL
HORIZONTAL

SWITCH
22
13
161
46
generar-trafico?
generar-trafico?
0
1
-1000

SLIDER
166
85
338
118
renovacion-aire
renovacion-aire
0.1
2
1.2
0.1
1
NIL
HORIZONTAL

@#$#@#$#@
## WHAT IS IT?

Este modelo simula el funcionamiento de una Zona de Bajas Emisiones (ZBE) en un entorno urbano. Representa cómo una barrera de control puede filtrar el tráfico rodado en función de las emisiones de CO2 de cada vehículo, permitiendo el paso solo a aquellos que cumplan con el límite establecido. También modela la contaminación atmosférica generada por los vehículos en las calles y su dispersión natural.


## HOW IT WORKS

El mundo se divide en una cuadrícula con una zona verde central (ZBE) y cuatro carriles verticales por los que circulan los vehículos de sur a norte. Los vehículos se generan en la parte inferior del mapa con uno de tres tipos de motor: eléctrico, híbrido o de combustión, en proporciones aproximadas al parque automovilístico español (DGT 2024), aunque con eléctricos e híbridos ligeramente inflados para mayor visibilidad.

Cada vehículo tiene asignado un consumo de combustible aleatorio dentro de un rango realista según su tipo, y a partir de este se calcula su generación de CO2 en g/km. Cuando un vehículo en los carriles centrales alcanza la frontera sur de la ZBE (y = -8), se evalúa si sus emisiones superan el límite configurado. Si las supera, es desviado lateralmente hacia los carriles exteriores, rodeando la ZBE por los lados y reincorporándose más adelante. Si las emisiones están dentro del límite, el vehículo cruza la ZBE con normalidad.

La contaminación se acumula en los parches por los que circulan vehículos de combustión, y se dispersa progresivamente en cada tick gracias al mecanismo de renovación de aire.


## HOW TO USE IT

Botones:
- Setup: Inicializa el mundo, borra los vehículos y reinicia todos los contadores.
- Go: Arranca la simulación en modo continuo.

Switches:
- generar-trafico?: Activa o desactiva la generación de nuevos vehículos durante la simulación.

Sliders:
- intensidad-trafico (1-10): Controla la frecuencia de generación de vehículos. A mayor valor, más vehículos por tick.
- limite-emisiones: Umbral de CO2 (g/km) por encima del cual un vehículo es rechazado en la barrera ZBE.
- renovacion-aire: Velocidad a la que se disipa la contaminación acumulada en los parches de carretera.

Monitores:
- tur-totales: Total de vehículos generados desde el inicio.
- cuenta-entrados: Vehículos que han cruzado la ZBE.
- cuenta-desviados-barrera: Vehículos rechazados en la barrera por exceso de emisiones.

Colores de los vehículos:
- Azul      -> Eléctrico
- Verde     -> Híbrido
- Rojo      -> Combustión


## THINGS TO NOTICE

- Observa cómo la zona verde central permanece visualmente limpia mientras que las calles exteriores acumulan tonos amarillos/naranjas conforme aumenta la contaminación.
- Con el límite de emisiones por defecto, la gran mayoría de vehículos rojos (combustión) son desviados, mientras que los azules y verdes cruzan libremente.
- Los vehículos desviados realizan una maniobra en U: giran lateralmente al ser rechazados, ascienden por los carriles exteriores y se reincorporan al flujo normal pasada la ZBE.
- A intensidades de tráfico altas, pueden formarse colas cuando varios vehículos coinciden en el mismo parche y el movimiento se bloquea momentáneamente.


## THINGS TO TRY

- Sube el límite de emisiones hasta un valor muy alto (ej. 300 g/km): observa cómo casi todos los vehículos entran en la ZBE y la contaminación interior aumenta.
- Bájalo a 0: prácticamente todos los vehículos serán desviados.
- Combina una intensidad de tráfico alta con un límite de emisiones bajo y observa cómo se saturan los carriles de desvío.
- Ajusta la renovacion-aire para ver cómo una ciudad con mejor ventilación natural mantiene niveles de contaminación mucho más bajos.


## EXTENDING THE MODEL

- Añadir un sistema de multas o registro de matrículas para los vehículos que intenten saltarse la barrera.
- Incorporar semáforos en los cruces para simular retenciones reales.
- Modelar distintas franjas horarias con diferentes intensidades de tráfico (hora punta, nocturna, etc.).
- Incluir vehículos de emergencia (ambulancias, bomberos) con acceso prioritario a la ZBE independientemente de sus emisiones.
- Añadir una gráfica en tiempo real que muestre la evolución del porcentaje de vehículos admitidos vs. desviados.
- Modelar el impacto en la salud de los residentes dentro de la ZBE en función de la contaminación acumulada.


## NETLOGO FEATURES

- Se utiliza patches-own y turtles-own para almacenar variables individuales por agente, lo que permite una lógica distribuida sin variables globales innecesarias.
- El procedimiento mover-coche combina lógica condicional de desvío y avance en un único bloque, gestionando los estados mediante flags booleanos (desvio-izq?, desvio-der?, en-maniobra?).
- La visualización de la contaminación se realiza directamente manipulando pcolor con una escala numérica (rango 10-19.9), aprovechando la paleta de colores integrada de NetLogo.
- El uso de foreach sobre una lista fija de coordenadas [-12 -3 3 12] permite generar vehículos en múltiples carriles de forma compacta.
- La generación de tráfico está controlada con ticks mod frecuencia, una forma idiomática en NetLogo de crear eventos periódicos sin agentes adicionales.


## RELATED MODELS

- Traffic Basic (NetLogo Models Library): Modelo fundamental de tráfico unidireccional con agentes vehículo.
- Traffic Grid (NetLogo Models Library): Simulación de tráfico en una cuadrícula urbana con semáforos.
- Air Pollution (NetLogo Models Library): Modela la dispersión de contaminantes en el aire de forma similar al mecanismo de contaminacion usado aquí.


## CREDITS AND REFERENCES

Modelo desarrollado como simulación educativa de políticas de movilidad urbana sostenible.
Datos de referencia para proporciones de vehículos: DGT - Informe del Parque de Vehículos 2024 (https://www.dgt.es).
Factores de emisión de CO2 basados en metodología WLTP estándar europea.

Para más información sobre Zonas de Bajas Emisiones en España, consultar:
- Real Decreto-ley 7/2022 sobre requisitos de ZBE en municipios de más de 50.000 habitantes.
- https://www.miteco.gob.es

Modelo realizado por: Ismael Aliaño Garzón, Mario Valdés Cáceres, Alejandro Macías del Pozo, Alejandro Quintero Vergara y Elías Vargas Ruíz. 

Todos los derechos reservados. Se admite la modificación en su totalidad siempre que se haga referencia y se aporten los créditos a nuestra obra.
@#$#@#$#@
default
true
0
Polygon -7500403 true true 150 5 40 250 150 205 260 250

airplane
true
0
Polygon -7500403 true true 150 0 135 15 120 60 120 105 15 165 15 195 120 180 135 240 105 270 120 285 150 270 180 285 210 270 165 240 180 180 285 195 285 165 180 105 180 60 165 15

arrow
true
0
Polygon -7500403 true true 150 0 0 150 105 150 105 293 195 293 195 150 300 150

box
false
0
Polygon -7500403 true true 150 285 285 225 285 75 150 135
Polygon -7500403 true true 150 135 15 75 150 15 285 75
Polygon -7500403 true true 15 75 15 225 150 285 150 135
Line -16777216 false 150 285 150 135
Line -16777216 false 150 135 15 75
Line -16777216 false 150 135 285 75

bug
true
0
Circle -7500403 true true 96 182 108
Circle -7500403 true true 110 127 80
Circle -7500403 true true 110 75 80
Line -7500403 true 150 100 80 30
Line -7500403 true 150 100 220 30

butterfly
true
0
Polygon -7500403 true true 150 165 209 199 225 225 225 255 195 270 165 255 150 240
Polygon -7500403 true true 150 165 89 198 75 225 75 255 105 270 135 255 150 240
Polygon -7500403 true true 139 148 100 105 55 90 25 90 10 105 10 135 25 180 40 195 85 194 139 163
Polygon -7500403 true true 162 150 200 105 245 90 275 90 290 105 290 135 275 180 260 195 215 195 162 165
Polygon -16777216 true false 150 255 135 225 120 150 135 120 150 105 165 120 180 150 165 225
Circle -16777216 true false 135 90 30
Line -16777216 false 150 105 195 60
Line -16777216 false 150 105 105 60

car
false
0
Polygon -7500403 true true 300 180 279 164 261 144 240 135 226 132 213 106 203 84 185 63 159 50 135 50 75 60 0 150 0 165 0 225 300 225 300 180
Circle -16777216 true false 180 180 90
Circle -16777216 true false 30 180 90
Polygon -16777216 true false 162 80 132 78 134 135 209 135 194 105 189 96 180 89
Circle -7500403 true true 47 195 58
Circle -7500403 true true 195 195 58

circle
false
0
Circle -7500403 true true 0 0 300

circle 2
false
0
Circle -7500403 true true 0 0 300
Circle -16777216 true false 30 30 240

cow
false
0
Polygon -7500403 true true 200 193 197 249 179 249 177 196 166 187 140 189 93 191 78 179 72 211 49 209 48 181 37 149 25 120 25 89 45 72 103 84 179 75 198 76 252 64 272 81 293 103 285 121 255 121 242 118 224 167
Polygon -7500403 true true 73 210 86 251 62 249 48 208
Polygon -7500403 true true 25 114 16 195 9 204 23 213 25 200 39 123

cylinder
false
0
Circle -7500403 true true 0 0 300

dot
false
0
Circle -7500403 true true 90 90 120

face happy
false
0
Circle -7500403 true true 8 8 285
Circle -16777216 true false 60 75 60
Circle -16777216 true false 180 75 60
Polygon -16777216 true false 150 255 90 239 62 213 47 191 67 179 90 203 109 218 150 225 192 218 210 203 227 181 251 194 236 217 212 240

face neutral
false
0
Circle -7500403 true true 8 7 285
Circle -16777216 true false 60 75 60
Circle -16777216 true false 180 75 60
Rectangle -16777216 true false 60 195 240 225

face sad
false
0
Circle -7500403 true true 8 8 285
Circle -16777216 true false 60 75 60
Circle -16777216 true false 180 75 60
Polygon -16777216 true false 150 168 90 184 62 210 47 232 67 244 90 220 109 205 150 198 192 205 210 220 227 242 251 229 236 206 212 183

fish
false
0
Polygon -1 true false 44 131 21 87 15 86 0 120 15 150 0 180 13 214 20 212 45 166
Polygon -1 true false 135 195 119 235 95 218 76 210 46 204 60 165
Polygon -1 true false 75 45 83 77 71 103 86 114 166 78 135 60
Polygon -7500403 true true 30 136 151 77 226 81 280 119 292 146 292 160 287 170 270 195 195 210 151 212 30 166
Circle -16777216 true false 215 106 30

flag
false
0
Rectangle -7500403 true true 60 15 75 300
Polygon -7500403 true true 90 150 270 90 90 30
Line -7500403 true 75 135 90 135
Line -7500403 true 75 45 90 45

flower
false
0
Polygon -10899396 true false 135 120 165 165 180 210 180 240 150 300 165 300 195 240 195 195 165 135
Circle -7500403 true true 85 132 38
Circle -7500403 true true 130 147 38
Circle -7500403 true true 192 85 38
Circle -7500403 true true 85 40 38
Circle -7500403 true true 177 40 38
Circle -7500403 true true 177 132 38
Circle -7500403 true true 70 85 38
Circle -7500403 true true 130 25 38
Circle -7500403 true true 96 51 108
Circle -16777216 true false 113 68 74
Polygon -10899396 true false 189 233 219 188 249 173 279 188 234 218
Polygon -10899396 true false 180 255 150 210 105 210 75 240 135 240

house
false
0
Rectangle -7500403 true true 45 120 255 285
Rectangle -16777216 true false 120 210 180 285
Polygon -7500403 true true 15 120 150 15 285 120
Line -16777216 false 30 120 270 120

leaf
false
0
Polygon -7500403 true true 150 210 135 195 120 210 60 210 30 195 60 180 60 165 15 135 30 120 15 105 40 104 45 90 60 90 90 105 105 120 120 120 105 60 120 60 135 30 150 15 165 30 180 60 195 60 180 120 195 120 210 105 240 90 255 90 263 104 285 105 270 120 285 135 240 165 240 180 270 195 240 210 180 210 165 195
Polygon -7500403 true true 135 195 135 240 120 255 105 255 105 285 135 285 165 240 165 195

line
true
0
Line -7500403 true 150 0 150 300

line half
true
0
Line -7500403 true 150 0 150 150

pentagon
false
0
Polygon -7500403 true true 150 15 15 120 60 285 240 285 285 120

person
false
0
Circle -7500403 true true 110 5 80
Polygon -7500403 true true 105 90 120 195 90 285 105 300 135 300 150 225 165 300 195 300 210 285 180 195 195 90
Rectangle -7500403 true true 127 79 172 94
Polygon -7500403 true true 195 90 240 150 225 180 165 105
Polygon -7500403 true true 105 90 60 150 75 180 135 105

plant
false
0
Rectangle -7500403 true true 135 90 165 300
Polygon -7500403 true true 135 255 90 210 45 195 75 255 135 285
Polygon -7500403 true true 165 255 210 210 255 195 225 255 165 285
Polygon -7500403 true true 135 180 90 135 45 120 75 180 135 210
Polygon -7500403 true true 165 180 165 210 225 180 255 120 210 135
Polygon -7500403 true true 135 105 90 60 45 45 75 105 135 135
Polygon -7500403 true true 165 105 165 135 225 105 255 45 210 60
Polygon -7500403 true true 135 90 120 45 150 15 180 45 165 90

sheep
false
15
Circle -1 true true 203 65 88
Circle -1 true true 70 65 162
Circle -1 true true 150 105 120
Polygon -7500403 true false 218 120 240 165 255 165 278 120
Circle -7500403 true false 214 72 67
Rectangle -1 true true 164 223 179 298
Polygon -1 true true 45 285 30 285 30 240 15 195 45 210
Circle -1 true true 3 83 150
Rectangle -1 true true 65 221 80 296
Polygon -1 true true 195 285 210 285 210 240 240 210 195 210
Polygon -7500403 true false 276 85 285 105 302 99 294 83
Polygon -7500403 true false 219 85 210 105 193 99 201 83

square
false
0
Rectangle -7500403 true true 30 30 270 270

square 2
false
0
Rectangle -7500403 true true 30 30 270 270
Rectangle -16777216 true false 60 60 240 240

star
false
0
Polygon -7500403 true true 151 1 185 108 298 108 207 175 242 282 151 216 59 282 94 175 3 108 116 108

target
false
0
Circle -7500403 true true 0 0 300
Circle -16777216 true false 30 30 240
Circle -7500403 true true 60 60 180
Circle -16777216 true false 90 90 120
Circle -7500403 true true 120 120 60

tree
false
0
Circle -7500403 true true 118 3 94
Rectangle -6459832 true false 120 195 180 300
Circle -7500403 true true 65 21 108
Circle -7500403 true true 116 41 127
Circle -7500403 true true 45 90 120
Circle -7500403 true true 104 74 152

triangle
false
0
Polygon -7500403 true true 150 30 15 255 285 255

triangle 2
false
0
Polygon -7500403 true true 150 30 15 255 285 255
Polygon -16777216 true false 151 99 225 223 75 224

truck
false
0
Rectangle -7500403 true true 4 45 195 187
Polygon -7500403 true true 296 193 296 150 259 134 244 104 208 104 207 194
Rectangle -1 true false 195 60 195 105
Polygon -16777216 true false 238 112 252 141 219 141 218 112
Circle -16777216 true false 234 174 42
Rectangle -7500403 true true 181 185 214 194
Circle -16777216 true false 144 174 42
Circle -16777216 true false 24 174 42
Circle -7500403 false true 24 174 42
Circle -7500403 false true 144 174 42
Circle -7500403 false true 234 174 42

turtle
true
0
Polygon -10899396 true false 215 204 240 233 246 254 228 266 215 252 193 210
Polygon -10899396 true false 195 90 225 75 245 75 260 89 269 108 261 124 240 105 225 105 210 105
Polygon -10899396 true false 105 90 75 75 55 75 40 89 31 108 39 124 60 105 75 105 90 105
Polygon -10899396 true false 132 85 134 64 107 51 108 17 150 2 192 18 192 52 169 65 172 87
Polygon -10899396 true false 85 204 60 233 54 254 72 266 85 252 107 210
Polygon -7500403 true true 119 75 179 75 209 101 224 135 220 225 175 261 128 261 81 224 74 135 88 99

wheel
false
0
Circle -7500403 true true 3 3 294
Circle -16777216 true false 30 30 240
Line -7500403 true 150 285 150 15
Line -7500403 true 15 150 285 150
Circle -7500403 true true 120 120 60
Line -7500403 true 216 40 79 269
Line -7500403 true 40 84 269 221
Line -7500403 true 40 216 269 79
Line -7500403 true 84 40 221 269

wolf
false
0
Polygon -16777216 true false 253 133 245 131 245 133
Polygon -7500403 true true 2 194 13 197 30 191 38 193 38 205 20 226 20 257 27 265 38 266 40 260 31 253 31 230 60 206 68 198 75 209 66 228 65 243 82 261 84 268 100 267 103 261 77 239 79 231 100 207 98 196 119 201 143 202 160 195 166 210 172 213 173 238 167 251 160 248 154 265 169 264 178 247 186 240 198 260 200 271 217 271 219 262 207 258 195 230 192 198 210 184 227 164 242 144 259 145 284 151 277 141 293 140 299 134 297 127 273 119 270 105
Polygon -7500403 true true -1 195 14 180 36 166 40 153 53 140 82 131 134 133 159 126 188 115 227 108 236 102 238 98 268 86 269 92 281 87 269 103 269 113

x
false
0
Polygon -7500403 true true 270 75 225 30 30 225 75 270
Polygon -7500403 true true 30 75 75 30 270 225 225 270
@#$#@#$#@
NetLogo 6.3.0
@#$#@#$#@
@#$#@#$#@
@#$#@#$#@
@#$#@#$#@
@#$#@#$#@
default
0.0
-0.2 0 0.0 1.0
0.0 1 1.0 0.0
0.2 0 0.0 1.0
link direction
true
0
Line -7500403 true 150 150 90 180
Line -7500403 true 150 150 210 180
@#$#@#$#@
0
@#$#@#$#@
