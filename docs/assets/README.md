# PayWave diagram assets

Use these files in the repo and for social posts (GitHub, LinkedIn).

| File | Use for |
|------|---------|
| [`paywave-microservices-architecture.svg`](./paywave-microservices-architecture.svg) | **Application** layer: 5 services, databases, flows (README, technical posts) |
| [`paywave-aws-devops-architecture.svg`](./paywave-aws-devops-architecture.svg) | **AWS + DevOps** target: ALB, EKS, RDS, ECR, Jenkins, SonarQube, Trivy, Argo CD |
| [`../../image.png`](../../image.png) | Simple “what the app does” overview (non-technical audience) |

## GitHub

In `README.md`:

```markdown
![PayWave microservices](docs/assets/paywave-microservices-architecture.svg)
![PayWave on AWS](docs/assets/paywave-aws-devops-architecture.svg)
```

GitHub renders SVG inline in the README.

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