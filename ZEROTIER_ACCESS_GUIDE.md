# 🔐 Remote Access via ZeroTier — Setup Guide

## How It Works

Your CatchUP POS app now includes an **embedded ZeroTier node**. When enabled, the POS tablet joins your ZeroTier virtual network and gets a private IP address (e.g., `10.147.17.55`). Any device on the same ZeroTier network can reach the POS at that IP from **anywhere in the world** — no port forwarding, no public IP required.

```
┌──────────────────────┐         ZeroTier P2P         ┌──────────────────────┐
│  POS Tablet (Home)   │◄─────────Encrypted──────────►│  Remote Device       │
│  IP: 10.147.17.55    │         Tunnel               │  (Anywhere)          │
│  Port: 8080          │                               │                      │
└──────────────────────┘                               └──────────────────────┘
```

---

## Step 1: Create a ZeroTier Network

1. Go to [my.zerotier.com](https://my.zerotier.com) and sign in
2. Click **Create A Network**
3. Copy the **Network ID** (16 hex characters, e.g. `1a2b3c4d5e6f7890`)
4. Note: The network starts as **Public** (any device can join). For security, set it to **Private** after setup.

---

## Step 2: Configure the POS App

1. Open CatchUP POS → **Settings** → **Remote Access (ZeroTier)**
2. Paste your **Network ID** in the field
3. (Optional) Enter your **API Key** for auto-authorization:
   - Go to [my.zerotier.com/account](https://my.zerotier.com/account)
   - Scroll to **API Access Tokens** → **Generate New Token**
   - Copy the token and paste it in the app
4. Toggle **Auto-connect on startup** ON
5. Tap **Connect**

The app will:
- Start the ZeroTier node
- Join your network
- Request authorization (auto-authorized if API key is set)
- Display a **Virtual IP** (e.g., `10.147.17.55`)

**Copy this IP** — you'll need it on remote devices.

---

## Step 3: Connect Remote Devices

### 📱 Android Device

1. Install **ZeroTier One** from [Google Play](https://play.google.com/store/apps/details?id=com.zerotier.one)
2. Open the app → Enter the same **Network ID**
3. Tap **Join**
4. If the network is Private, authorize the device on [my.zerotier.com](https://my.zerotier.com) (Network → Members)
5. Open a browser or KDS app → Connect to `10.147.17.55:8080`

### 🍎 iOS Device

1. Install **ZeroTier** from the [App Store](https://apps.apple.com/app/zerotier/id1154844942)
2. Open the app → Enter the same **Network ID**
3. Tap **Join** and approve the VPN permission
4. Authorize the device if network is Private
5. Connect to the POS at the virtual IP

### 🖥️ Windows / Mac / Linux

1. Download ZeroTier from [zerotier.com/download](https://www.zerotier.com/download/)
2. Install and open the ZeroTier client
3. Right-click the tray icon → **Join New Network** → Enter the **Network ID**
4. Authorize the device if network is Private
5. Open browser → Navigate to `http://10.147.17.55:8080`

### 🌐 Web Browser (from any ZeroTier-connected device)

Any device running the ZeroTier client can access the POS via its virtual IP. Simply open a browser and navigate to:

```
http://<VIRTUAL_IP>:8080
```

Replace `<VIRTUAL_IP>` with the IP shown in the POS app settings.

---

## Network Security

### Public vs Private Networks

| Mode | Behavior |
|------|----------|
| **Public** | Any device with the Network ID can join instantly. Good for testing. |
| **Private** | Each device must be authorized by the network owner on my.zerotier.com. Recommended for production. |

### Recommended Setup

1. Start with **Public** to test connectivity
2. Switch to **Private** once everything works
3. Configure **Auto-authorization** via API key so the POS tablet always joins
4. Manually authorize remote devices as needed

### API Key Auto-Authorization

If you set an API key in the POS app, it will automatically authorize itself on the network. This means:
- The POS always reconnects on restart
- You don't need to manually authorize it
- Remote devices still need manual authorization (for security)

---

## Troubleshooting

### "Network join timed out"

- The device is not authorized. Go to [my.zerotier.com](https://my.zerotier.com) → Network → Members → Authorize the device.
- If using API key, check that it has the correct permissions.

### Can't connect from remote device

- Ensure ZeroTier is running on both devices
- Verify both devices are on the same network (check Network ID)
- Try pinging the POS virtual IP from the remote device
- Check that the KDS server is running on the POS

### IP address not showing

- The ZeroTier node may still be starting. Wait 10-15 seconds.
- Check the app logs for ZeroTier errors.
- Ensure the VPN permission was granted when prompted.

### Connection drops frequently

- ZeroTier uses P2P which can be affected by NAT/firewalls.
- The connection will auto-reconnect. If it persists, try relaying through ZeroTier's root servers (automatic).

---

## Architecture

```
POS Tablet                          Remote Device
┌─────────────────────┐            ┌─────────────────────┐
│ CatchUP POS App     │            │ ZeroTier Client     │
│  ├─ ZeroTier Node   │◄──────────►│  (App/System)       │
│  │  (libzt)         │  P2P       │                     │
│  │  IP: 10.x.x.x    │  Encrypted │  Browser/App        │
│  │                   │            │  → http://10.x:8080 │
│  └─ KDS Server :8080│            └─────────────────────┘
└─────────────────────┘
```

- ZeroTier creates a **virtual Ethernet switch** over the internet
- All traffic is **encrypted end-to-peer** (not routed through servers)
- Most connections are **direct P2P**; falls back to relaying if needed
- Free tier: 25 devices per network, unlimited bandwidth
