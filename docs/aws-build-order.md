# AWS build order (what to create, in sequence)

Use this **top to bottom**. **EKS, Helm, and Argo CD come last**, after network, RDS, and CI basics exist.

Related: [aws-architecture.md](aws-architecture.md) (what each box is) · [aws-devops-roadmap.md](aws-devops-roadmap.md) (tools).

---

## Phase 0 — Before Terraform (one-time)

| Step | Create | Why |
| --- | --- | --- |
| 0.1 | AWS account + billing alarm | Avoid surprise cost |
| 0.2 | IAM admin user (not root) + MFA | Safe console/API access |
| 0.3 | Choose **one region** (e.g. `ap-south-1`) | Everything lives here first |
| 0.4 | Git repo for **Terraform** (separate from PAYWAVE app) | Infra as code |
| 0.5 | PayWave app **Dockerfiles** + smoke test on EC2 (done) | Images ready for ECR later |

---

## Phase 1 — Terraform foundation (no EKS yet)

| Order | Create with Terraform | Notes |
| --- | --- | --- |
| **1** | **S3 bucket** (Terraform state) | Versioning on |
| **2** | **DynamoDB table** (state lock) | `LockID` key |
| **3** | **VPC** `10.0.0.0/16` | One VPC per environment |
| **4** | **2 public subnets** (2 AZs) | e.g. `10.0.1.0/24`, `10.0.2.0/24` |
| **5** | **2 private subnets** (2 AZs) | e.g. `10.0.11.0/24`, `10.0.12.0/24` |
| **6** | **Internet Gateway** + attach to VPC | |
| **7** | **Elastic IP** + **NAT Gateway** (public subnet) | Dev: 1 NAT is OK |
| **8** | **Route tables**: public → IGW; private → NAT | Two route table patterns |
| **9** | **Security groups** (skeleton) | `alb-sg`, `app-sg`, `rds-sg` (rules tightened later) |

**Checkpoint:** VPC console shows public + private subnets and NAT working (private subnet can reach internet via NAT).

---

## Phase 2 — Data layer (still no EKS)

| Order | Create | Notes |
| --- | --- | --- |
| **10** | **DB subnet group** (private subnets only) | |
| **11** | **RDS MySQL 8** (single-AZ for dev) | |
| **12** | Four schemas/DBs: `account_db`, `wallet_db`, `payment_db`, `settlement_db` | Flyway on first app start |
| **13** | **Secrets Manager** secrets | DB user/password, later JWT + internal API key |
| **14** | **RDS security group**: allow **3306** only from app SG (EC2 or EKS nodes later) | No public RDS |

**Checkpoint:** Connect to RDS from a **temporary** bastion/EC2 or Session Manager test instance in private subnet.

---

## Phase 3 — Run PayWave without Kubernetes (optional but recommended)

Bridge step so you are not blocked on EKS.

| Order | Create | Notes |
| --- | --- | --- |
| **15** | **EC2** in private subnet (or one small public for dev only) | Run your 5 JARs or future Docker compose |
| **16** | **ACM certificate** (DNS validation) | For `xyz.com` |
| **17** | **Route 53** record → **ALB** | |
| **18** | **Application Load Balancer** (public subnets) | Listener **443** → target group → gateway **8080** |
| **19** | Point apps to **RDS** (not Docker MySQL on EC2) | Same env vars as EC2 today |

**Checkpoint:** `https://xyz.com/` → demo UI; `./scripts/e2e-smoke.sh` passes.

---

## Phase 4 — Registry and CI (still no EKS)

| Order | Create | Notes |
| --- | --- | --- |
| **20** | **ECR repositories** (5) | `api-gateway`, `account-service`, `wallet-service`, `payment-service`, `settlement-service` |
| **21** | **IAM** for Jenkins/CI to push ECR | |
| **22** | **EC2 for Jenkins** (private + SSM) | **Ansible** installs Jenkins |
| **23** | **SonarQube** (SonarCloud or EC2) | Quality gate in pipeline |
| **24** | Jenkins pipeline: `mvn test` → SonarQube → `docker build` → **Trivy** → **push ECR** | No deploy to K8s yet |

**Checkpoint:** Every service has an image in ECR with a known tag (e.g. `1.0.0`).

---

## Phase 5 — GitOps repo (Helm charts, still no cluster)

| Order | Create | Notes |
| --- | --- | --- |
| **25** | Git repo (or folder) **`paywave-helm`** | Charts + `values-dev.yaml` |
| **26** | **Helm chart** per service or one umbrella chart | Deployments, Services, ConfigMaps |
| **27** | **External Secrets** manifest templates | Pull from Secrets Manager |
| **28** | **Ingress** manifest for **api-gateway** only | Host + TLS annotation for ALB controller |

**Checkpoint:** `helm template` renders valid YAML locally; image tags point at ECR.

---

## Phase 6 — EKS (now)

| Order | Create with Terraform | Notes |
| --- | --- | --- |
| **29** | **EKS cluster** (private API optional later; public API OK for dev) | |
| **30** | **EKS node group** in **private subnets** | Start small: 1–2 nodes, `t3.large` or `t3.medium` |
| **31** | **OIDC provider** + **IRSA** roles | ALB controller, External Secrets, etc. |
| **32** | EKS addons: **VPC CNI**, CoreDNS, kube-proxy | |
| **33** | Install **AWS Load Balancer Controller** (Helm) | Connects Ingress to your existing ALB pattern |
| **34** | **Security groups**: node SG ↔ RDS SG (3306) | |

**Checkpoint:** `kubectl get nodes` shows Ready; cluster can pull from ECR.

---

## Phase 7 — Argo CD + deploy (last)

| Order | Create | Notes |
| --- | --- | --- |
| **35** | Install **Argo CD** on EKS (Helm) | GitOps controller |
| **36** | Register **paywave-helm** Git repo in Argo CD | |
| **37** | Argo **Application** for `paywave` namespace | Sync Helm chart |
| **38** | Jenkins job: after ECR push, **update image tag in Git** (Helm values) | Argo syncs automatically |
| **39** | Scale **2 replicas** per service | Your HA plan |
| **40** | **CloudWatch** log groups / Container Insights (optional) | |

**Checkpoint:** `https://xyz.com/` hits **gateway on EKS**; smoke test green; Argo shows `Synced`.

---

## One-line summary

```text
S3/DynamoDB (state) → VPC/subnets/NAT/SG → RDS + Secrets → ALB+ACM+Route53 (+ EC2 JARs optional)
→ ECR → Jenkins+Sonar+Trivy → Helm charts in Git → EKS cluster + nodes → Argo CD → deploy
```

---

## What you deliberately do **not** do first

| Do not start with | Why |
| --- | --- |
| EKS | Needs VPC, subnets, RDS, images in ECR |
| Argo CD | Needs EKS + Helm charts in Git |
| S3 `java -jar` on workers | Wrong model for Kubernetes; use **ECR images** |
| Public **RDS** | Security |
| **web-ui-service** on AWS | UI is in **api-gateway** image |

---

## Terraform module order (folder names you will create)

1. `backend` (S3 + DynamoDB) — often bootstrap manually once  
2. `network` (VPC, subnets, NAT, IGW, routes)  
3. `security-groups`  
4. `rds`  
5. `secrets`  
6. `ecr`  
7. `alb` + `acm` + `route53`  
8. `ec2-jenkins` (optional)  
9. `eks` (cluster + node group + IRSA)  
10. `iam-ci` (Jenkins ECR push)

Helm/Argo are **not** Terraform in the app flow—they are **Git + kubectl/Helm** (or Argo after step 35).

---

## When you are on EC2 today

You already did **Phase 0 app** + MySQL in Docker. Your next Terraform win is **Phase 1–2** (VPC + RDS), then **Phase 3** (ALB to EC2), then containerize → **Phase 4–7**.