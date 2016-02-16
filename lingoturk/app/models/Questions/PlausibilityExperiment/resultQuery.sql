SELECT number,condition,workerId,PlausibilityResult.answer
FROM PlausibilityResult
JOIN Questions USING (QuestionId);