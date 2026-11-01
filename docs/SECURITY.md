# Security boundaries

The default Audius/local-library mode has no account credentials. Music HTTP clients do not carry Supabase headers. Cleartext HTTP and application backup are disabled. Download filenames are hashed, written to temporary private files, checked for completeness, and renamed only after successful transfer. Downloads are bounded to 250 MiB.

The retained university authentication is unsafe: passwords are stored/compared by a public table API, and a local user ID is not a cryptographic session. It is opt-in for debug builds only. Public profile requests explicitly omit password and email columns; passwords are no longer stored in SavedStateHandle. These mitigations do not secure a previously deployed database or erase old logs.

Before any public authenticated release:

1. Replace legacy login/register with Supabase Auth and token refresh/sign-out handling.
2. Migrate identities and password storage through an owner-approved backend process.
3. Audit grants/RLS for profiles, messages, follows, playlists, likes, and RPCs using multiple users.
4. Enforce entitlements on a trusted backend, never a client-written flag.
5. Review existing deployments and rotate any previously exposed privileged keys.

Do not embed service-role JWTs, `sb_secret_` keys, or database passwords. Public client keys are extractable from APKs and require RLS; see [Supabase API security](https://supabase.com/docs/guides/api/securing-your-api).

No production credentials, database migrations, or remote data mutations were needed for the default catalog integration.

Ktor is aligned to 2.3.13, the patched 2.x version for [CVE-2024-49580](https://github.com/advisories/GHSA-8qv4-773j-c979). The affected HttpCache plugin was not explicitly installed by this app; this update removes the affected dependency version. This is a targeted advisory review, not a claim that every transitive dependency is vulnerability-free.
