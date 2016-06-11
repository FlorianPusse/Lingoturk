SELECT Plausibility_number, Plausibility_condition, workerId, PlausibilityResults.answer
FROM PlausibilityResults
JOIN Questions USING (QuestionId);