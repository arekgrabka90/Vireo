#!/data/data/com.termux/files/usr/bin/bash
set -e
pkg install -y git >/dev/null
[ -d .git ] || git init -b main
git add -A
git commit -m "Vireo: faza 0 (szkielet)" || true
if ! git remote | grep -q origin; then
  read -p "URL repo GitHub (https://github.com/USER/vireo.git): " URL
  git remote add origin "$URL"
fi
git push -u origin main
echo "Gotowe. Zakladka Actions na GitHubie zbuduje APK."
