SELECT fileName, listNumber, AskQuestions_scenario, assignmentId, hitId, workerId, origin, timestamp, partId, questionId, answer FROM (
	SELECT * FROM AskQuestionsResults
	LEFT OUTER JOIN Questions USING (QuestionId)
	LEFT OUTER JOIN Groups USING (PartId)
) as tmp
WHERE LingoExpModelId = 21
ORDER BY partId, questionId, workerId