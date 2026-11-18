# PayWave diagram assets

Use these files in the repo and for social posts (GitHub, LinkedIn).

| File | Use for |
|------|---------|
| [`paywave-microservices-architecture.png`](./paywave-microservices-architecture.png) | **GitHub README** — application layer (renders reliably) |
| [`paywave-aws-devops-architecture.png`](./paywave-aws-devops-architecture.png) | **GitHub README** — AWS + DevOps target |
| [`paywave-microservices-architecture.svg`](./paywave-microservices-architecture.svg) | Editable vector (Inkscape / browser) |
| [`paywave-aws-devops-architecture.svg`](./paywave-aws-devops-architecture.svg) | Editable vector |
| [`../../image.png`](../../image.png) | Simple “what the app does” overview (non-technical audience) |

## GitHub

The README uses **PNG** files. GitHub often shows *Invalid image source* for embedded **SVG** in README; use PNG for the repo, SVG for editing.

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