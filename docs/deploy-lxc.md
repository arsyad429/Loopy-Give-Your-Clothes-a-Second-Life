# Deploy the Go API with Docker inside LXC

This guide assumes a **Proxmox unprivileged LXC running Ubuntu 24.04/26.04 or Debian 12/13**, with systemd and a reachable LAN IP. Run the Linux commands as root **inside the LXC**, except the explicitly marked Proxmox host command. If Docker already works there, skip its installation.

The repository provides [`compose.lxc.yaml`](../compose.lxc.yaml), a standalone deployment file for the Go API and PostgreSQL. It adds restart policies, bounded container logs, a required database password, and a configurable API bind address. PostgreSQL has persistent storage and no published port. The Android build container is not included. Auth is the only implemented business feature; see [auth-go.md](auth-go.md).

No remote deployment has been performed by Codex. Docker/LXC runtime behavior must be verified on your server using the checks below.

## 1. Prepare the LXC

For a small development deployment that builds the Go image locally, start with 2 CPU cores, 4 GiB RAM, and 20 GiB disk, then adjust after measuring your workload. Give the LXC a stable LAN IPv4 through a static address or DHCP reservation, working DNS, and outbound internet access.

In Proxmox, enable **Options -> Features -> Nesting and keyctl** on the container. Keep the other existing feature settings. Apply the change while the container is stopped, then start it. Proxmox documents `keyctl` for Docker in unprivileged containers and `nesting` for nested workloads: [official container feature reference](https://github.com/proxmox/pve-docs/blob/master/generated/pct.conf.5-opts.adoc).

Enable **Start at boot** for this LXC. The Docker services will also restart when Docker starts, unless you explicitly stopped them.

For a NEW container with no other feature settings, the equivalent command on the **Proxmox host**, using example container ID 101, is:

```sh
pct set 101 --features nesting=1,keyctl=1
```

Replace `101` with your container ID. This sets the feature list; use the UI to preserve any other flags on an existing container. The Docker installation and all remaining commands belong inside the LXC, not on the Proxmox host. Other LXC managers such as Incus/LXD have different host-side settings.

## 2. Install Docker Engine and Compose

These commands adapt Docker's official apt-repository instructions for [Ubuntu](https://docs.docker.com/engine/install/ubuntu/) and [Debian](https://docs.docker.com/engine/install/debian/). They assume a fresh container without conflicting Docker/containerd packages.

```sh
apt-get update
apt-get install -y ca-certificates curl git openssl nano
```

Open Bash (`bash`) and run:

```bash
. /etc/os-release
case "$ID" in
  ubuntu) docker_distro=ubuntu; docker_codename="${UBUNTU_CODENAME:-$VERSION_CODENAME}" ;;
  debian) docker_distro=debian; docker_codename="$VERSION_CODENAME" ;;
  *) echo 'Use the Docker installation guide for your distribution'; exit 1 ;;
esac

install -m 0755 -d /etc/apt/keyrings
curl -fsSL "https://download.docker.com/linux/$docker_distro/gpg" -o /etc/apt/keyrings/docker.asc
chmod a+r /etc/apt/keyrings/docker.asc
cat > /etc/apt/sources.list.d/docker.sources <<EOF
Types: deb
URIs: https://download.docker.com/linux/$docker_distro
Suites: $docker_codename
Components: stable
Architectures: $(dpkg --print-architecture)
Signed-By: /etc/apt/keyrings/docker.asc
EOF

apt-get update
apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
systemctl enable --now docker
docker compose version
docker run --rm hello-world
```

Continue only after `hello-world` runs. If Docker fails before it can start a container, inspect `systemctl status docker`, `journalctl -u docker -n 100 --no-pager`, and the Proxmox feature settings. Runtime/mount permission errors belong to the LXC/Docker host configuration. Check the host/runtime versions and the exact error before changing isolation settings.

## 3. Put the current source on the LXC

The Go auth implementation and deployment files must first be committed and pushed to the branch you will deploy, or copied directly to the LXC. **A clone of the older scaffold will not contain uncommitted local changes.** No commit/push is performed by this guide.

Once your deployment branch is available remotely:

```sh
mkdir -p /opt/loopy
git clone https://github.com/arsyad429/Loopy-Give-Your-Clothes-a-Second-Life.git /opt/loopy
cd /opt/loopy
# If the implementation is on another branch, switch to it here.
test -f backend/go/main.go
test -f compose.lxc.yaml
```

Clone into an empty directory. For a private repository use your normal SSH key or Git credential flow; keep access tokens out of clone URLs and shell history.

## 4. Configure the database password and network binding

From `/opt/loopy`, copy the server example if `.env` does not already exist:

```sh
test -f .env || cp docker/lxc.env.example .env
chmod 600 .env
openssl rand -hex 32
nano .env
```

Use the generated value for `POSTGRES_PASSWORD`. This file is ignored by Git. Choose the binding that matches your setup:

| Setup | `API_BIND_ADDRESS` | Client or proxy upstream |
| --- | --- | --- |
| Reverse proxy installed directly in the same LXC | `127.0.0.1` | `http://127.0.0.1:8080` |
| LAN testing or reverse proxy on another machine/LXC | This API LXC's LAN IPv4, e.g. `192.168.1.50` | `http://192.168.1.50:8080` |

Example for LAN testing; substitute your actual IP and generated password:

```dotenv
POSTGRES_DB=loopy
POSTGRES_USER=loopy
POSTGRES_PASSWORD=<generated-password>
API_PORT=8080
API_BIND_ADDRESS=192.168.1.50
GROQ_API_KEY=
GROQ_MODEL=qwen/qwen3.8-27b
GROQ_RESPONSE_MODE=json_schema
```

The bind address is the **LXC's IP**, not its Proxmox host's IP or Docker bridge IP. A loopback-published port is reachable from the LXC itself; another machine needs the LAN binding. See [Docker port publishing](https://docs.docker.com/engine/network/port-publishing/).

Allow the chosen API port from your development devices or reverse proxy in the Proxmox/network firewall. Docker's published ports can bypass ordinary UFW rules; use the outer firewall or Docker-aware filtering for access restrictions ([Docker's firewall notes](https://docs.docker.com/engine/install/ubuntu/#firewall-limitations)). PostgreSQL remains internal on `db:5432`.

Database credentials initialize an empty PostgreSQL volume. Changing `POSTGRES_PASSWORD` in `.env` after initialization does not change the existing database role's password; rotate the database password and application settings together ([official PostgreSQL image documentation](https://github.com/docker-library/docs/blob/master/postgres/README.md)).

## 5. Build, start, and check the services

```sh
cd /opt/loopy
docker compose -f compose.lxc.yaml config --quiet
docker compose -f compose.lxc.yaml up --build -d --wait
docker compose -f compose.lxc.yaml ps
```

The first build downloads Go dependencies and images. The API runs account/session migrations automatically. There is no separate SQL import or Go installation on the LXC.

For LAN binding, verify inside the LXC, replacing the example IP:

```sh
curl -fsS http://192.168.1.50:8080/health
curl -fsS http://192.168.1.50:8080/ready
```

Use `127.0.0.1` instead when you selected loopback binding. Expected responses are `{"status":"ok","mode":"go-auth"}` and `{"status":"ready"}`. Repeat the LAN check from your laptop to verify routing/firewalls. Loopback binding intentionally has no direct remote access.

If startup fails:

```sh
docker compose -f compose.lxc.yaml logs --tail=100 backend db
```

## 6. Create an admin account if needed

No default user or admin password is seeded. In Bash, enter a password privately and pass it to the one-off CLI:

```bash
export LOOPY_ADMIN_NAME='Loopy Admin'
export LOOPY_ADMIN_EMAIL='admin@example.com'
read -r -s -p 'Admin password (15–128 characters): ' LOOPY_ADMIN_PASSWORD
printf '\n'
export LOOPY_ADMIN_PASSWORD
docker compose -f compose.lxc.yaml run --rm \
  -e LOOPY_ADMIN_NAME -e LOOPY_ADMIN_EMAIL -e LOOPY_ADMIN_PASSWORD \
  backend create-admin
unset LOOPY_ADMIN_PASSWORD
```

Use an email you control. The command refuses an existing email. Users register through `POST /v1/auth/register`. See [the auth guide](auth-go.md) for JSON examples and the full register/login/refresh/logout flow.

## 7. Connect Android or a public domain

For LAN development, set Android's base URL to `http://192.168.1.50:8080/`, using your actual LXC IP and the trailing slash. The app's debug build permits HTTP. `10.0.2.2` refers to the emulator's host computer and does not identify the remote LXC. A host Android build can use:

```sh
cd android
./gradlew assembleDebug -PLOOPY_API_BASE_URL=http://192.168.1.50:8080/
```

Docker on the LXC runs the API; Android remains on your emulator/device. Setting `LOOPY_API_BASE_URL` on the server does not reconfigure an APK already installed on a phone. API deployment also does not implement the pending Android auth screens/repository wiring.

For public use, terminate HTTPS at your reverse proxy and set Android to `https://api.your-domain.com/`. For example, with Caddy installed directly in this same LXC, leave `API_BIND_ADDRESS=127.0.0.1` and configure:

```caddyfile
api.your-domain.com {
    reverse_proxy 127.0.0.1:8080
}
```

Point the domain's DNS to the public entry point and route TCP 80/443 to Caddy for its normal automatic certificate flow. Configure only DNS records your server can actually serve, including IPv6 if you publish an AAAA record. Installation and certificate details are in the [Caddy reverse proxy guide](https://caddyserver.com/docs/quick-starts/reverse-proxy). If your connection is behind CGNAT, inbound router port forwarding alone will not provide a public entry point; use your existing reachable reverse proxy or tunnel.

A proxy running in another Docker container cannot reach this API using its own `127.0.0.1`; use a shared Docker network with upstream `backend:8080`, or the API LXC's reachable LAN address. A proxy on another LXC also uses the LAN address. Backend code currently rate-limits by socket peer IP, so traffic through a proxy shares that peer's auth limit (20 attempts/minute). Plan gateway rate limiting/trusted-proxy handling before a multi-user public deployment.

## 8. Updates and backups

After your team pushes a reviewed change to the deployed branch:

```sh
cd /opt/loopy
git pull --ff-only
docker compose -f compose.lxc.yaml up --build -d --wait
docker compose -f compose.lxc.yaml ps
```

These steps can briefly interrupt the API. The existing `postgres-data` named volume retains accounts and sessions. Keep the Compose project name `loopy` and volume name stable. Normal `stop`/`down` keeps the volume; `down -v` deletes database storage.

For a database backup on the LXC, run from the repository root:

```sh
umask 077
mkdir -p /opt/loopy-backups
docker compose -f compose.lxc.yaml exec -T db \
  sh -c 'pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" -Fc' \
  > "/opt/loopy-backups/loopy-$(date -u +%Y%m%dT%H%M%SZ).dump"
```

Check the command succeeds, copy backups off the LXC, and test restoration in a separate database. Keep PostgreSQL on major version 17 until you plan a database upgrade; pulling a newer major image is not a data migration. Docker restart behavior is described in the [official restart-policy documentation](https://docs.docker.com/engine/containers/start-containers-automatically/).
