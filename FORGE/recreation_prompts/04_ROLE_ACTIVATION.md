# 04 — Role Activation (Layer 12)

```json
{
  "phases": {
    "20_data_model":        { "lead": "senior_backend_engineer",  "support": ["senior_solutions_architect"] },
    "21_ux_flows":          { "lead": "senior_ux_researcher",     "support": ["senior_product_manager"] },
    "22_uiux_design":       { "lead": "senior_ui_ux_designer",    "support": ["senior_design_system_lead", "senior_accessibility_specialist"] },
    "23_api_contract":      { "lead": "senior_backend_engineer",  "support": ["senior_solutions_architect"] },
    "24_art_direction":     { "lead": "senior_graphic_designer",  "support": ["senior_brand_strategist"] },
    "30_frontend_build":    { "lead": "senior_frontend_engineer", "support": ["senior_ui_ux_designer", "senior_qa_engineer"] },
    "40_features":          { "lead": "senior_fullstack_engineer","support": ["senior_qa_engineer", "senior_security_engineer"] },
    "50_governance":        { "lead": "senior_security_engineer", "support": ["senior_product_manager"] },
    "60_observability":     { "lead": "senior_devops_engineer",   "support": ["senior_qa_engineer"] }
  },
  "councilRules": {
    "lead": "owns the artifact and the final call",
    "support": "surface risks and trade-offs before finalize",
    "conflictResolution": "highest user+business value at lowest acceptable risk; name the trade-off; one unified recommendation"
  },
  "escalations": [
    "Production lock changes require owner role; demo impersonation is acknowledged as non-security boundary.",
    "Any future real-HTTP execution must add CORS + secret handling review before merge."
  ]
}
```
