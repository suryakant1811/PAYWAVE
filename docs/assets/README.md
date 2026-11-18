# PayWave diagram assets

Use these files in the repo and for social posts (GitHub, LinkedIn).

| File | Use for |
|------|---------|
| [`paywave-microservices-architecture.png`](./paywave-microservices-architecture.png) | **GitHub README** — application layer (renders reliably) |
| [`paywave-aws-devops-architecture.png`](./paywave-aws-devops-architecture.png) | **GitHub README** — AWS + DevOps target |
| [`paywave-aws-full-architecture.png`](./paywave-aws-full-architecture.png) | Full AWS VPC, AZ, subnet, EKS, RDS and CI/CD view |
| [`paywave-microservices-architecture.svg`](./paywave-microservices-architecture.svg) | Editable vector (Inkscape / browser) |
| [`paywave-aws-devops-architecture.svg`](./paywave-aws-devops-architecture.svg) | Editable vector |
| [`paywave-aws-full-architecture.svg`](./paywave-aws-full-architecture.svg) | Editable full AWS vector |
| [`../../image.png`](../../image.png) | Simple “what the app does” overview (non-technical audience) |

## GitHub

The README uses **PNG** for inline images. GitHub often shows *Invalid image source* for `![](*.svg)` in README (security sanitizer).

**See SVG locally (Cursor):**

1. Open `docs/assets/preview.html` in a browser (best check before push).
2. Or open `paywave-*.svg` → right-click → **Open Preview** / built-in SVG preview.

**See SVG on GitHub:** click the `.svg` file in the repo (GitHub renders the file viewer). Do not rely on README `![](file.svg)`.

```markdown
![PayWave microservices](docs/assets/paywave-microservices-architecture.png)
![PayWave on AWS](docs/assets/paywave-aws-devops-architecture.png)
```

**Mermaid** blocks in the README also render on GitHub (no image file needed).

## LinkedIn (PNG export)

SVG is best in the repo; LinkedIn prefers a raster image (~1200×627 or 1080×1080).

**On your Mac** (after opening the SVG in a browser or using a tool):

1. Open the `.svg` in Chrome or Firefox (full width).
2. Screenshot at 2× zoom, or export with [Inkscape](https://inkscape.org/) / Illustrator: **Export PNG** at 1200 px width.

**On EC2 / Linux** (optional):

```bash
# if rsvg-convert is installed: dnf install librsvg2-tools
rsvg-convert -w 1200 docs/assets/paywave-aws-devops-architecture.svg \
  -o docs/assets/paywave-aws-devops-architecture-1200w.png
```

Post **one** diagram per slide/carousel item:

1. Microservices (what you built + smoke-tested).
2. AWS target (what you are building with Terraform).

## Suggested LinkedIn caption (short)

> Built **PayWave** — a Paytm-style wallet backend in **Java 21 / Spring Boot**: API gateway, account, wallet, ledger, payments, refunds, and merchant settlement. Each service owns its own **MySQL** schema. Public entry through **api-gateway** with JWT + demo UI. Next: **Terraform, EKS, RDS, GitOps (Helm + Argo CD)**, **Jenkins** with **SonarQube** and **Trivy**.  
> Repo: https://github.com/suryakant1811/PAYWAVE

## Hashtags (pick 5–8)

`#Java #SpringBoot #Microservices #AWS #Kubernetes #DevOps #Terraform #Backend`