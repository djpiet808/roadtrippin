# Roadtrippin privacy route

This directory is a self-contained static privacy and account-deletion route for the existing Bent Nail Studio Netlify site.

Copy `roadtrippin/privacy/` and the matching `_headers` rules into the existing site's publish directory. Do not deploy this directory alone to the production site, because a manual Netlify deploy replaces the site's complete published output.

Local check:

```sh
python3 -m http.server 4173 --directory privacy-site
curl --fail --head http://127.0.0.1:4173/roadtrippin/privacy/
```

Required production result:

```sh
curl --fail --head https://bentnail.studio/roadtrippin/privacy
```
