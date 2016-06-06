SELECT number,condition,workerId,PlausibilityResults.answer
FROM PlausibilityResults
JOIN Questions USING (QuestionId);