# HTTPS setup with Let's Encrypt (Certbot)

This document describes steps to obtain TLS certificates using Certbot and Let's Encrypt on the VPS.

1. DNS
   - Create an A record for your API domain (e.g. api.your-domain.com) pointing to the VPS public IP.

2. Install Certbot (Ubuntu example)
   sudo apt update
   sudo apt install -y snapd
   sudo snap install core; sudo snap refresh core
   sudo snap install --classic certbot
   sudo ln -s /snap/bin/certbot /usr/bin/certbot

3. Obtain certificate (nginx)
   sudo certbot certonly --nginx -d api.your-domain.com

   or use the standalone plugin if nginx is not running on 80/443 during issuance:
   sudo certbot certonly --standalone -d api.your-domain.com

4. Certificates are saved under /etc/letsencrypt/live/api.your-domain.com/

5. Configure nginx to use:
   ssl_certificate /etc/letsencrypt/live/api.your-domain.com/fullchain.pem;
   ssl_certificate_key /etc/letsencrypt/live/api.your-domain.com/privkey.pem;

6. Automatic renewal (installed by snap):
   sudo systemctl enable --now snap.certbot.renew.service
   # Test renewal
   sudo certbot renew --dry-run

Notes:
- Make sure port 80 is reachable when issuing a certificate using the nginx plugin or standalone.
- Use DNS challenge if your environment blocks incoming 80/443.

