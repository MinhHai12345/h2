# Model Evaluation

## Prediction snapshots

Each READY Match Analytics calculation completed strictly before kickoff is recorded as an immutable prediction snapshot. A new calculation creates a new snapshot; an identical match/provider/model-version/computation-time tuple is idempotent.

The snapshot preserves the original home/draw/away probabilities. Rebuilding Match Analytics never overwrites an earlier snapshot. Snapshots with a computation time at or after kickoff, or with INSUFFICIENT_DATA status, are not recorded.

## Evaluation metrics

- **Multiclass Brier Score:** sum of squared errors across HOME, DRAW and AWAY. The score ranges from 0 to 2; lower is better.
- **Log Loss:** negative natural logarithm of the probability assigned to the actual outcome. A floor of 1e-15 prevents an infinite result when the model assigns zero probability.
- **Calibration:** snapshots are grouped into ten buckets by maximum predicted outcome probability. Each bucket reports average confidence, observed accuracy and sample count.

Reports are grouped by model version. Results from different model versions must not be combined into a single aggregate. Calibration is descriptive and should be interpreted with sample size in mind.

## Evaluation rules

- Only FINISHED matches with both final scores are evaluated.
- Unfinished matches remain pending.
- Re-running evaluation is idempotent for an unchanged final score.
- If an authoritative final score is corrected, the stored outcome and metrics are recalculated from the same immutable probability snapshot.
- All timestamps use the application's UTC clock.

## API

- GET /api/v1/seasons/{seasonId}/model-evaluation?provider=SOFASCORE
- POST /api/v1/seasons/{seasonId}/model-evaluation/rebuild?provider=SOFASCORE (requires X-Ingestion-Admin-Key)

The rebuild operation evaluates every eligible pending or score-corrected snapshot in the requested season and provider.
