# Preuve de concept

> [Dépôt GitHub du projet](https://github.com/DontBeAJerry/Application_Complexe)

## Introduction 

Dans un cadre de suivi de la population, une des premières idées est d'utiliser la géolocalisation telle que déjà présente dans notre quotidien. Pour beaucoup de personnes, la géolocalisation est activée à chaque instant. Seulement ce système nous permet de suivre la tendance des déplacements d'une personne, mais nous sommes limités dans la précision. En effet, la triangulation par GPS  possède une imprécision de quelques mètres, pour les plus performant (dépendant de l'environnement). 

Il est intéressant de se demander comment améliorer cette précision grâce aux autres connexions présentes sur un téléphone (mobile, Wi-Fi, NFC, Bluetooth, etc.) pour pouvoir détecter et mesurer les contacts entre des personnes dans les espaces publiques. 

Google nous indique par ailleurs que son service Maps utilise plusieurs types de connexion pour estimer la position exacte de l'utilisateur :

> ##### Comment votre position actuelle est-elle déterminée dans Maps ?
>
> Google Maps procède à une estimation de votre position à partir des sources suivantes :
>
> - **GPS** : grâce à des satellites, votre position est détectée avec une marge d'erreur maximale de 20 mètres (**remarque** : lorsque vous êtes à l'intérieur de bâtiments ou sous terre, il se peut que votre position GPS ne soit pas exacte).
> - **Wi-Fi** : l'emplacement des réseaux Wi-Fi alentour aide Maps à vous situer.
> - **Relais de téléphonie mobile** : la précision d'une connexion à un réseau mobile est de quelques kilomètres.
>
> [Déterminer votre position et en améliorer la précision](https://support.google.com/maps/answer/2839911?co=GENIE.Platform%3DAndroid&hl=fr) - *support.google.com*



Nous voyons alors qu'il est impératif, pour améliorer la précision, de coupler la géolocalisation GPS à d'autre type de communication. 

L'idée ici est alors de montrer que nous pouvons utiliser le Bluetooth pour déterminer un contact sensible et potentiellement risqué entre deux utilisateurs en calculant la distance les séparant. 



## Etat de l'art

### Bluetooth

Le Bluetooth est une norme de communication permettant l'échange bidirectionnel de données à très courte distance en utilisant des ondes radio UHF sur une bande de fréquence de 2,4 GHz. Sa portée est donc limité dans l'espace et dépend de plusieurs facteurs.

#### 1. La couche physique

La couche physique (PHY) définit le schéma de modulation et les autres techniques qu'elle utilise pour envoyer des données sur une bande de fréquences radio (RF) spécifique. 
Cela inclut le nombre de canaux disponibles, l'efficacité avec laquelle ces canaux sont utilisés, l'utilisation de la correction d'erreur, les gardes en place pour contrer les interférences, et bien plus encore. 

Les deux ont un impact sur la portée à laquelle vous pouvez être entendu.

#### 2. La sensibilité du récepteur 

La sensibilité du récepteur, ou RSSI, est la mesure de la puissance minimale du signal qu'un récepteur peut interpréter. En d'autres termes, il s'agit du niveau de puissance le plus faible auquel le récepteur peut détecter un signal radio, maintenir une connexion et continuer à démoduler des données. 

La norme Bluetooth précise que les appareils doivent pouvoir atteindre une sensibilité minimale du récepteur de -70 dBm à -82 dBm, selon le PHY utilisé. Cependant, les mises en œuvre de Bluetooth permettent généralement d'atteindre des niveaux de sensibilité de réception beaucoup plus élevés. Par exemple, les implémentations moyennes du Bluetooth LE 125K (Coded) PHY atteignent une sensibilité de réception de -103 dBm.

#### 3. La puissance de transmission

Le choix d'un niveau de puissance d'émission est un compromis de conception entre la portée et la consommation d'énergie. Plus la puissance d'émission est élevée, plus le signal est susceptible d'être entendu à longue distance et plus la portée effective est grande. Cependant, l'augmentation de la puissance d'émission augmente la consommation d'énergie de votre appareil. 

Le Bluetooth prend en charge des puissances d'émission allant de -20 dBm (0,01 mW) à +20 dBm (100 mW).

#### 4. Le gain d'antenne

L'antenne convertit l'énergie électrique de l'émetteur en énergie électromagnétique (ou ondes radio) et vice-versa pour le récepteur. L'emplacement de l'antenne, la taille du produit et la conception peuvent avoir une grande influence sur l'efficacité de la transmission et de la réception du signal. De plus, les antennes et leur efficacité à convertir l'énergie électrique en énergie électromagnétique ainsi qu'à concentrer la direction de l'énergie peuvent varier considérablement.

Les concepteurs de Bluetooth peuvent choisir de mettre en œuvre une variété d'options d'antenne. La conception d'antennes est autant un art qu'une science. Les appareils Bluetooth atteignent généralement un gain d'antenne compris entre -10 dBi et +10 dBi.

#### 5. L'affaiblissement de propagation 

L'affaiblissement de propagation est la réduction de la force du signal qui se produit lorsqu'une onde radio se propage dans l'air. L'affaiblissement de propagation, ou atténuation de propagation, se produit naturellement sur une certaine distance et est influencé par l'environnement dans lequel le signal est transmis. Les obstacles entre l'émetteur et le récepteur peuvent détériorer le signal.

Les atténuateurs peuvent être n'importe quoi, de l'humidité et des précipitations aux murs, fenêtres et autres obstacles en verre, bois, métal ou béton, y compris les tours ou panneaux métalliques qui réfléchissent et diffusent les ondes radio. Bien que les ondes radio puissent traverser des objets, le degré d'affaiblissement et l'affaiblissement de propagation effectif varient en fonction du type et de la densité de l'obstacle. 

> D'après le Bluetooth Special Interest Group (SIG), les modèles de calculs différent en fonction de l'environnement. 
>
> En extérieur, le modèle "Two-Rays Ground Reflected Model" est utilisé tandis qu'en milieu industriel est utilisé le "Log-distance path loss model". 
>
> [Path Loss (Propagation) Models](https://www.bluetooth.com/wp-content/uploads/Files/Marketing/range-assumptions.pdf) - Bluetooth SIG



### Calcul de la distance

La norme Bluetooth précise  : 

<p align="center">
    <a href="https://www.codecogs.com/eqnedit.php?latex=\dpi{150}&space;RSSI&space;=&space;TxPower&space;-&space;10*N*lg(d)" target="_blank"><img src="https://latex.codecogs.com/png.latex?\dpi{150}&space;-30dBm\geq&space;RSSI&space;=&space;txPower&space;-&space;10*N*lg(d)&space;\geq-90dBm" title="RSSI = TxPower - 10*N*lg(d)" /></a>
</p>  


Par conséquent nous avons :

<p align="center"><a style="align:center" href="https://www.codecogs.com/eqnedit.php?latex=\dpi{150}&space;d&space;\approx&space;10^{\frac{txPower&space;-&space;RSSI}{10*N}}" target="_blank"><img src="https://latex.codecogs.com/png.latex?\dpi{150}&space;d&space;\approx&space;10^{\frac{txPower&space;-&space;RSSI}{10*N}}" title="d = 10^{\frac{txPower - RSSI}{10*N}}" /></a></p>

Avec : 

- txPower, la puissance de transmission en dBm
- RSSI, la puissance de réception du signal en dBm
- N, l'indice d'affaiblissement de la propagation (N = 2 dans un espace ouvert sans obstacle) 

> Pour la suite des calculs nous fixerons N à 4 car les tests effectuer sont situés en maison. Cependant, il faudrait trouver un moyen de détecter l'environnement de l'utilisateur, par exemple grâce à la géolocalisation GPS.



<p align="center"><img src="https://i.ibb.co/0V5pF9q/Screenshot-1.png" alt="Screenshot-1" border="0"></p>

<p align="center">Graphique de portée du Bluetooth en fonction du RSSI dans un espace habitable</p>



Grâce au graphique ci-dessus, nous pouvons observer que la distance théorique par le calcul est comprise entre 1,7m et 56,2m. Bien sûr, cette courbe n'est qu'une tendance générale car elle ne prend pas en compte tout les paramètres du Bluetooth. 



## Implémentation 

Pour démontrer la faisabilité technique de cette idée, je suis passé par deux projets semblables mais n'utilisant pas les mêmes technologies. En effet, de nombreuses limitations techniques se retrouvent lors de l'implémentation. 

Le principe de chaque application se rapproche du processus suivant : 

1. Accéder à l'API Bluetooth du terminal en cours d'utilisation
2. Scanner le réseau environnant et afficher les terminaux rencontrés
3. Récupérer les informations importantes à l'estimation de la distance
4. Estimation de la distance

> Nous considérons qu'une rencontre est sans risque au-dessus d'une distance de 2 mètres.



### Programme Java Desktop

> [Dépôt GitHub de l'application](https://github.com/DontBeAJerry/Application_Complexe/tree/master/AppComplexe)

Cette partie est la plus triviale. Nous utilisons un langage disposant d'API très large permettant d'utiliser énormément de fonctionnalités de l'appareil, ici le Bluetooth. 

<p align="center"><img src="https://i.ibb.co/3zQ8PyV/desktop-App-Result.png" alt="desktop-App-Result" border="0"></p>

<p align="center">Output du Scan Bluetooth</p>

Nous détectons bien les terminaux autour de nous. Cependant, nous n'obtenons ni la puissance de transmission ni le RSSI qui est à 0. 

Ce problème est localisé à la première ligne : 

> Native Library blucove_x64 not available

Cette erreur nous indique, d'après la documentation, l'impossibilité d'utiliser une autre pile Bluetooth que la pile Winsock. Cette pile est la pile par défaut sur la majorité des ordinateurs sous Windows. Or, Winsock ne dispose pas d'API lui permettant de récupérer le RSSI d'un terminal, ce qui nous lève une `NotSupportedIOException`. Il faut alors changer la pile Winsock pour, par exemple, une pile Widcomm. 

Pour contrer cette limitation technique, l'idéal est de passer directement par un smartphone. Pour se faire, une application cross-plateforme peut-être utilisée. 

### Application Flutter + Flutter Blue

> [Dépôt GitHub de l'application](https://github.com/DontBeAJerry/Application_Complexe/tree/master/AppComplexe)

Flutter Blue est une API permettant la gestion du Bluetooth via le Framework Flutter. 



<p align="center"><img src="https://i.ibb.co/JBNNr4X/Screenshot-20200522-120653-com-pauldemarco-flutter-blue-example.jpg" alt="Screenshot-20200522-120653-com-pauldemarco-flutter-blue-example" border="0" height="500"></p>

<p align="center">Application d'estimation de distance</p>

Le terminal qui nous intéresse ici est celui ayant l'adresse `42:62:D9:7C:C8:89`. 

Nous pouvons voir sur la colonne de gauche le RSSI mesuré, ici -23dBm et sur la colonne de droite la distance estimée. Sa description nous indique que l'ID du constructeur est `4C`, ce qui correspond à un appareil Apple d'après [l'identifiant constructeur Bluetooth](https://www.bluetooth.com/specifications/assigned-numbers/company-identifiers/).

D'après notre estimation, les deux terminaux se trouvent à 1,19m de distance. Cependant, ils sont en réalité espacé de quelques centimètres. Nous remarquons alors la limite de notre estimation : certains paramètres ne sont pas pris en compte :

- Le gain des antennes d'émission et de réception

Et d'autre ne sont pas assez précis : 

- La puissance de transmission car impossible à récupérer. Cette donnée est fixée en dure mais elle dépend du modèle de l'antenne Bluetooth et donc du téléphone.

- Le RSSI fluctuant sans changer de distance, à cause du bruit ambiant. 

  <p align="center"><img src="https://i.ibb.co/CtJkhMW/rssi-fluctuation.png" alt="rssi-fluctuation" border="0" height="500"></p>

<p align="center">Différence de 40cm sans déplacer les appareils</p>

Dans un cas applicatif concret, nous obtenons les mesures suivantes : 

| RSSI mesurée (dBm) | Distance mesurée (cm) | Distance théorique (cm) |
| ------------------ | --------------------- | ----------------------- |
| -23                | 10                    | 119                     |
| -29                | 20                    | 168                     |
| -30                | 30                    | 178                     |
| -33                | 40                    | 211                     |
| -34                | 50                    | 224                     |
| -41                | 60                    | 335                     |
| -45                | 70                    | 422                     |
| -47                | 80                    | 473                     |
| -50                | 90                    | 562                     |
| -51                | 100                   | 596                     |



<p align="center"><img src="https://i.ibb.co/82Wyx8F/Screenshot-5.png" alt="Screenshot-5" border="0"></p>

<p align="center">Graphiques des valeurs mesurée et théoriques par rapport à la fonction distance</p>

On remarque que les valeurs mesurées sont très loin des valeurs théoriques. Ce qui semble cohérent du fait que le modèle utilisé est trivial.  



## Conclusion

Nous avons vu qu'il était possible de scanner le réseau Bluetooth environnant pour récupérer certaines informations sans avoir à demander une autorisation de connexion. Ces informations peuvent être utilisées pour identifier une entité, échanger des données, ou estimer la distance séparant deux entités. 

Cependant, certains problèmes persistent et des axes d'amélioration sont possibles. 

- Pour contrer la fluctuation du RSSI, il serait possible de calculer une moyenne sur un court laps de temps. Cela permettrait d'avoir une donnée plus fiable et moins soumise au bruit ambiant. 

- La puissance de transmission devrait être possible à récupérer. Cependant de nombreuses différences matérielles peuvent impacter cette données. En effet, certains constructeurs semblent ne pas donner accès à cette valeur dans la trame de données échangée.

- Les valeurs de gains d'antennes semblent être impactées du même problème que la puissance de transmission. Ces valeurs dépendent du constructeur, mais aussi du téléphone et de sa génération. 



Enfin, il semble tout à fait réalisable d'estimer la distance entre deux appareils par Bluetooth. Cependant, du fait de la complexité des spécifications techniques, il faut mettre en place un modèle mathématique beaucoup plus précis et prenant en compte davantage de paramètres (Angles du diffuseur, angle du récepteur, environnement, bruit ambiant,  ) 
