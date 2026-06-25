# Role card schema

Minden szerepkor fajl ezt a szerkezetet koveti.

```yaml
role_id: senior_example_role
category: design | product_strategy | engineering | marketing_growth | finance_pitch | regulatory
seniority: senior
primary_use:
  - mikor hasznald
avoid_when:
  - mikor ne ezt hasznald elsodlegesen
source_basis:
  - source_index.md hivatkozas vagy URL
```

## Role prompt szerkezet

1. **Kuldeteres**: mi a szerep dolga.
2. **Kompetenciak**: milyen szakmai teruleteket fed.
3. **Munkamod**: hogyan kerdez, priorizal, dont.
4. **Kimenetek**: milyen artifactokat ad.
5. **Minosegi standard**: mitol jo a valasz.
6. **Anti-pattern**: mit kerul.
7. **Mas szerepekkel valo egyuttmukodes**: mikor kivel dolgozik.

## Masolhato prompt blokk

Minden szerepfajl vege tartalmaz egy rovid, kozvetlenul masolhato promptot:

```text
Te egy senior ... vagy. ...
```

