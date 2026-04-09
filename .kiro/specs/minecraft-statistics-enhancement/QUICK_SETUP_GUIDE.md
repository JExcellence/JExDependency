# Quick Setup Guide - Making Statistics Work

## Current Status

✅ **Frontend is ready** - All code fixes are complete
⚠️ **Backend connection required** - Statistics need RCentral API to work

## Option 1: Connect to Production Backend (Recommended)

If you have access to the production RaindropCentral backend:

### Step 1: Get Your API Key
1. Log into RaindropCentral website
2. Navigate to your server management page
3. Copy your API key

### Step 2: Connect Your Server
Run this command in-game or console:
```
/rc connect <your-api-key>
```

### Step 3: Verify Connection
Check the console for:
```
[INFO] Successfully connected to RaindropCentral
[INFO] Statistics delivery service created
[INFO] Statistics delivery service initialized and started
[INFO] Vanilla statistics collection service initialized
```

### Step 4: Test Statistics Collection
1. Join the server
2. Play for a bit (break blocks, kill mobs, etc.)
3. Check console for statistics being collected
4. Quit the server - should be instant (no freeze)

---

## Option 2: Use Development Mode (Local Testing)

If you're running a local backend for development:

### Step 1: Enable Development Mode
Edit `plugins/RCore/rcentral.yml`:
```yaml
backend:
  development-mode: true  # Uses localhost:3000
  url: "http://localhost:3000"  # Optional: custom URL
```

### Step 2: Start Your Local Backend
Make sure your backend is running on `localhost:3000`

### Step 3: Implement Backend Fix
Follow the instructions in `BACKEND_DESERIALIZATION_FIX.md` to add the custom deserializer to your backend's `StatisticEntry` model.

### Step 4: Connect
```
/rc connect <your-dev-api-key>
```

---

## Option 3: Disable Statistics (Temporary)

If you don't need statistics right now:

### Edit Statistics Config
`plugins/RCore/statistics-delivery.yml`:
```yaml
enabled: false
```

This will stop the warnings and disable statistics collection entirely.

---

## Troubleshooting

### "RCentral service not fully initialized"

**Cause**: Server hasn't been connected with `/rc connect` yet

**Solution**: Run `/rc connect <api-key>` with a valid API key

### "Statistics delivery service not available"

**Cause**: RCentral service isn't connected, so statistics can't be delivered

**Solution**: Connect to RCentral first (see Option 1 or 2)

### Backend Returns 400/500 Error

**Cause**: Backend can't deserialize the `StatisticEntry.value` field

**Solution**: Implement the backend fix in `BACKEND_DESERIALIZATION_FIX.md`

### Server Still Freezes on Player Quit

**Cause**: Old build without the Material DataFixer fix

**Solution**: Rebuild RCore with `./gradlew :RCore:build` and restart server

---

## What's Working Now

✅ Server starts without crashes
✅ Players can join/quit instantly (no freezes)
✅ Graceful degradation when RCentral unavailable
✅ Frontend serialization working correctly

## What Needs Backend Connection

⏳ Statistics collection and delivery
⏳ Droplet store integration
⏳ Cross-server synchronization
⏳ Player data persistence

---

## Quick Test Commands

Once connected, test with these commands:

```bash
# Check connection status
/rc status

# View statistics queue
/rcstats queue

# Force delivery
/rcstats deliver

# View metrics
/rcstats metrics

# Test vanilla statistics
/vanillastats collect <player>
/vanillastats view <player>
```

---

## Configuration Files

### RCore Config
`plugins/RCore/config.yml`:
```yaml
connection:
  server-uuid: "auto-generated"
  api-key: null  # Set by /rc connect
  minecraft-uuid: null  # Set automatically
  minecraft-username: null  # Set automatically

privacy:
  share-player-list: true
```

### RCentral Config
`plugins/RCore/rcentral.yml`:
```yaml
backend:
  development-mode: false
  url: ""  # Empty = auto-detect

droplet-store:
  enabled: true
  enabled-items:
    - skill-xp-rate-50-cookie
    - job-xp-rate-10-cookie
    # ... more items
```

### Statistics Config
`plugins/RCore/statistics-delivery.yml`:
```yaml
enabled: true
delivery-interval-seconds: 60
native-stat-collection-interval-seconds: 300

queue:
  max-size: 10000
  backpressure-warning-threshold: 7500
  backpressure-critical-threshold: 9000

filtering:
  collect-native-statistics: true
  collect-block-statistics: true
  collect-item-statistics: true
  collect-mob-statistics: true
```

### Vanilla Statistics Config
`plugins/RCore/vanilla-statistics.yml`:
```yaml
enabled: true

collection:
  mode: EVENT_DRIVEN  # or SCHEDULED or HYBRID
  scheduled-interval-minutes: 5
  
categories:
  general: true
  blocks: true
  items: true
  mobs: true
  travel: true
  interactions: true
```

---

## Next Steps

1. **Choose your option** (Production, Development, or Disable)
2. **Follow the steps** for your chosen option
3. **Test the connection** with `/rc status`
4. **Verify statistics** are being collected
5. **Check for errors** in console

If you encounter issues, check the troubleshooting section or review the detailed documentation in:
- `BACKEND_DESERIALIZATION_FIX.md` - Backend implementation guide
- `MATERIAL_DATAFIXER_DEADLOCK_FIX.md` - Server freeze fix details
- `SESSION_SUMMARY.md` - Complete overview of all fixes

---

## Summary

The statistics system is **ready to work** but requires a connection to the RaindropCentral backend. Once connected with `/rc connect <api-key>`, everything will function properly. The server is stable and won't freeze - the warnings you're seeing are just informational.
