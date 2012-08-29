
teleological-links.xls
======================
The teleological database contains, for approximately 350 artifacts (nouns),
an encoding of the purpose of that artifact. The encoding is a set of triples
of the form:
  <artifact>   action       <verb>
  ....
  <artifact>   <relation1>  <object1>
  <artifact>   <relationN>  <objectN>

denoting that <verb> is the typical intended activity (purpose) which the
artifact was designed for. <relationN> <objectN> describe the typical
objects involved in that activity, and the semantic relation of that object
to that activity. The semantic relations used in the database are as follows:

RELATION:	DESCRIPTION:
action		Describes the typical intended activity (purpose) which
		the artifact was designed for. 
		e.g., a bed is intended for (ACTION) sleeping

For this typical intended activity, there are 11 roles used to describe it.
Note these relations are between the artifact's activity (not the artifact)
and the object mentioned.

RELATION:	DESCRIPTION:
agent		a rester is a (typical) AGENT of sleeping on a bed
beneficiary	an audience is a (typical) BENEFICIARY of showing a movie
cause		tiredness is a (typical) CAUSE of sleeping on a bed
destination	a shore is a (typical) DESTINATION of sailing a boat
experiencer	a child is a (typical) EXPERIENCER of swinging on a swing
instrument	a gun is a (typical) INSTRUMENT of shooting a bullet
location	a bedroom is a (typical) LOCATION of sleeping on a bed
result		rest is a (typical) RESULT of sleeping on a bed
source		a shore is a (typical) SOURCE of sailing a boat
theme		a passenger is a (typical) THEME of transporting by boat
undergoer	a target is a (typical) UNDERGOER of shooting an arrow


