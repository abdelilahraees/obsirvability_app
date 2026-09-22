# Observability App

> **Démonstration complète d'observabilité** : Métriques + Traces + Logs, corrélés et visualisés dans Grafana — le tout à partir d'une simple API Spring Boot.

---

## 📌 Le projet en une phrase

Un micro e-commerce (auth JWT, produits, panier, checkout) utilisé comme **bac à sable pédagogique** pour montrer comment rendre une application Java *fully observable* avec **Micrometer + OpenTelemetry**, sans effort de code invasif.

**Le vrai produit, c'est l'observabilité. L'e-commerce n'est que le prétexte.**

---

## 🧠 L'idée : pourquoi ce projet ?

Dans la plupart des applications :

| Problème classique | Résultat |
|---|---|
| ❌ Logs écrits en *console* | impossibles à fouiller, pas de contexte métier |
| ❌ Métriques isolées | pas de lien avec le code qui les a provoquées |
| ❌ Traces séparées | on ne sait pas *qui* a déclenché *quoi* |
| ❌ Logs/traces déconnectés | debug long et fastidieux |

### Ce que ce projet démontre

1. **Instrumentation automatique** — Spring Boot s'instrumente tout seul (HTTP, JPA, Bean runtime) ; on ajoute juste quelques annotations `@Observed`.
2. **Les 3 piliers exportés ensemble** via OTLP :
   - 🔎 **Traces** → Grafana Tempo
   - 📈 **Métriques** → Grafana Mimir (Prometheus)
   - 📝 **Logs** → Grafana Loki
3. **Corrélation totale** — chaque log transporte `trace_id` / `span_id` : on clique d'un log vers sa trace, et d'une trace vers ses logs.
4. **Spans enrichies avec le métier** — on tagge les spans avec `user.email`, `product.name`, `qty`, `cart.total`… → recherche puissante dans Tempo.
5. **Dashboards conformes aux conventions OTel** (RED, histogrammes explicites).

---

## 🏗️ Architecture

```
                    ┌──────────────────────────────────────────────┐
                    │               Spring Boot ([:8079])          │
                    │                                              │
   Client ────────▶ │  Auth (JWT)  Products  Cart  Checkout        │
   (Bash/Swagger)   │                                              │
                    │  Micrometer Observation (@Observed)          │
                    │  OpenTelemetry SDK                           │
                    └──────────┬──────────┬──────────┬─────────────┘
                               │          │          │  OTLP (4318)
                    ┌──────────▼────┐ ┌────▼────┐ ┌──▼────────┐
                    │  Tempo (trac) │ │  Mimir  │ │   Loki    │
                    └──────────▲────┘ └────▲────┘ └──▲────────┘
                               │            │        │
                    ┌──────────┴────────────┴────────┴─────────┐
                    │                GRAFANA (:3000)           │
                    │        Explore / Dashboards RED          │
                    └──────────────────────────────────────────┘

                    PostgreSQL   ── persistance (produits, users, paniers)
                    Zipkin       ── backend de traces alternatif
```

---

## 🧰 Stack technique

| Composant | Rôle |
|---|---|
| ☕ **Java 21 + Spring Boot 4.x** | Application web (MVC) |
| 🎛️ **Micrometer** | Observation, tracing, métriques |
| 🛰️ **OpenTelemetry SDK** | Export OTLP (traces / métriques / logs) |
| 📊 **Grafana LGTM** | Suite complète : Loki, Grafana, Tempo, Mimir |
| 🕵️ **Zipkin** | Backend de tracing alternatif |
| 🐘 **PostgreSQL** | Persistance JPA |
| 🔐 **Spring Security + JWT (jjwt)** | Auth stateless, contrôle par rôle |
| 📚 **Springdoc OpenAPI** | Swagger UI |
| ✅ **Bean Validation** | Validation des `@RequestBody` |

---

## ✨ Fonctionnalités métier (prétexte pédagogique)

| Endpoint | Description |
|---|---|
| `POST /auth/register` | Inscription → retourne un JWT |
| `POST /auth/login` | Connexion → retourne un JWT |
| `GET /products` · `GET /products/{id}` | Catalogue (données pré-remplie par `DataSeed`) |
| `POST /products` | Création d'un produit |
| `GET /cart` | Consultation du panier de l'utilisateur courant |
| `POST /cart/items` | Ajout d'un article (gère les doublons) |
| `DELETE /cart/items/{productId}` | Retrait d'un article |
| `POST /cart/checkout` | Validation : vérifie le stock, décrémente le stock, vide le panier |

Toutes les routes métier sont protégées par **`ROLE_USER`** (JWT Bearer).

---

## 🔭 Le cœur du projet : l'observabilité

### 1. Traces (Grafana Tempo)

- **Spans HTTP automatiques** : une trace par requête entrante.
- **Spans métier nommées** via `@Observed` :
  - `auth.register`, `auth.login`, `cart.get`, `cart.addItem`, `cart.removeItem`, `cart.checkout`, `cart.service`, `product.service`.
- **Attributs métier** injectés sur les spans dans `CartServiceImpl` via `SpanTagger` :

```java
span.tag("user.email", email);
span.tag("product.id", productId.toString());
span.tag("product.name", product.getName());
span.tag("cart.total", String.valueOf(total));
```

> 💡 Exemple de recherche Tempo : `{ resource.service.name = "observability_app" && span.user.email = "mon@email.com" }`

### 2. Métriques (Grafana Mimir / Prometheus)

- Export OTLP vers Mimir **+** endpoint Prometheus natif (`/actuator/prometheus`).
- **Convention OTel respectée** : renommage automatique de `http.server.requests` → `http.server.request.duration` (`MetricsConfig`).
- Histogrammes **explicit bucket** requis par le dashboard *"RED Metrics"* de LGTM (configuré dans `application.properties`).

### 3. Logs (Grafana Loki)

- **Appender OpenTelemetry Logback** (`logback-spring.xml`) : chaque log est exporté en OTLP avec le contexte (MDC, code, trace).
- **Access log HTTP** (`HttpAccessLogFilter`) : méthode, URI, statut, durée, IP à chaque requête.

```log
GET /products -> 200 (12 ms) remote=127.0.0.1
```

- **Corrélation automatique** : `logging.pattern.correlation` = `${name},%X{traceId:-},%X{spanId:-}` → chaque log porte ses IDs de trace.

> 💡 Exemple de recherche Loki : `{service_name="observability_app"} |= "mon@email.com"`

### Le flux de bout en bout

1. Un utilisateur appelle `POST /cart/items`.
2. **Trace Tempo** : 1 trace = spans HTTP + `cart.addItem` + `cart.service` + accès JPA.
3. **Logs Loki** : *"cart add user=… product=… qty=…"* reliée à la même trace.
4. **Métriques Mimir** : latence / erreurs / saturation des `/cart/items`.
5. **Grafana** : on part d'un log → on ouvre sa trace → on voit ses tags métier et ses métriques.

---

## 🚀 Démarrage rapide

### Prérequis

- JDK 21
- Docker (LGTM + PostgreSQL + Zipkin)

### 1 — Lancer la stack d'observabilité

```bash
docker compose up -d
```

| Service | URL |
|---|---|
| Grafana (UI) | http://localhost:3000 |
| OTLP collector (4317 gRPC / 4318 HTTP) | — |
| PostgreSQL | `localhost:5432` |
| Zipkin (UI) | http://localhost:9411 |

> 🔐 Au 1ᵉʳ démarrage, Grafana génère le mot de passe `admin` dans **les logs du conteneur**.

### 2 — Lancer l'application

```bash
./mvnw spring-boot:run
```

### 3 — Vérifier

| Cible | URL |
|---|---|
| Swagger UI | http://localhost:8079/swagger-ui.html |
| Health check | http://localhost:8079/actuator/health |
| Métriques Prometheus | http://localhost:8079/actuator/prometheus |

### 4 — Générer du trafic (optionnel)

```bash
./scripts/e2e-test.sh mon@email.com
```

Simule : inscription → ajout de 2 produits au panier → consultation → checkout → contrôle du stock. Parfait pour remplir Tempo / Loki / Mimir.

---

## 📂 Arborescence

```
observability_app/
├── compose.yaml                  # LGTM + PostgreSQL + Zipkin
├── scripts/
│   └── e2e-test.sh               # Smoke test complet
└── src/main/
    ├── java/org/example/observability_app/
    │   ├── config/               # Observation, Métriques, SpanTagger, HTTP logs, OpenAPI, DataSeed
    │   ├── security/             # JWT (jjwt) + Spring Security
    │   ├── web/                  # Contrôleurs REST (auth, products, cart)
    │   ├── service/              # Logique métier instrumentée (@Observed)
    │   ├── repository/           # Spring Data JPA
    │   └── entity/               # User, Product, Cart, CartItem
    └── resources/
        ├── application.properties   # Config OTLP / Actuator / JWT
        └── logback-spring.xml       # Appender OpenTelemetry → Loki
```

### Fichiers clés de l'observabilité

| Fichier | Rôle |
|---|---|
| `application.properties` | Endpoints OTLP, sampling 100%, histogrammes, corrélation |
| `logback-spring.xml` | Appender OTLP avec capture MDC / code |
| `ObservationConfig` | Active `@Observed` (AOP) |
| `MetricsConfig` | Alignement des noms de métriques OTel |
| `SpanTagger` | Ajout d'attributs métier sur la span courante |
| `HttpAccessLogFilter` | Access log par requête HTTP |
| `OtelLogAppenderInstaller` | Branche le SDK OTel sur l'appender Logback |

---

## 📝 Points d'attention avant production

- ⚠️ `app.jwt.secret` dans `application.properties` est un placeholder → **à remplacer**.
- ⚠️ Images Docker utilisées en `:latest` → **à épingler**.
- ⚠️ Export OTLP sur `localhost:4318` → à remplacer par l'URL du collector dans un orchestrateur.
- ⚠️ `logging.level....security=DEBUG` → à désactiver en prod.

---

## 🎓 Ce que le projet vous apprend

- Utiliser **Micrometer Observation** (`@Observed`, `ObservedAspect`) sans casser le code métier.
- Configurer **Spring Boot 4 Actuator** pour exporter traces / métriques / logs en **OTLP**.
- **Corréler** les 3 piliers au travers d'un `trace_id` commun.
- **Enrichir** les traces avec des attributs métier pour des recherches et dashboards pertinents.
- Naviguer dans **Grafana LGTM** (Explore Tempo / Loki, dashboards Mimir).

---

## 🔗 Ressources

- [Spring Boot Reference — Observability](https://docs.spring.io/spring-boot/4.1.1/reference/actuator/observability.html)
- [Micrometer Tracing](https://docs.micrometer.io/tracing/reference/index.html)
- [Grafana OTel LGTM](https://grafana.com/oss/)
- [OpenTelemetry](https://opentelemetry.io/)
