SELECT fileName, listNumber, SentenceCompletion_story, SentenceCompletion_list, SentenceCompletion_itemNr, SentenceCompletion_itemLength, SentenceCompletion_itemType, assignmentId, hitId, workerId, origin, timestamp, partId, questionId, answer FROM (
	SELECT * FROM SentenceCompletionResults
	LEFT OUTER JOIN Questions USING (QuestionId)
	LEFT OUTER JOIN Groups USING (PartId)
) as tmp
WHERE LingoExpModelId = 761
ORDER BY partId, questionId, workerId