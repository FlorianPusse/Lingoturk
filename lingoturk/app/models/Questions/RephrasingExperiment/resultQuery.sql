SELECT Rephrasing_sentence1, fileName, Rephrasing_question1, Rephrasing_questionFirst1, Rephrasing_sentence2, Rephrasing_question2, Rephrasing_questionFirst2, assignmentId, hitId, timestamp, partId, questionId, choice1, choice2, answer1, answer2, readingTime1, readingTime2, startedLearning FROM (
	SELECT * FROM RephrasingResults
	LEFT OUTER JOIN Questions USING (QuestionId)
	LEFT OUTER JOIN Groups USING (PartId)
) as tmp
WHERE LingoExpModelId = 41
ORDER BY partId, questionId, workerId