# Food-Stuff Logs

My idea for this project is to make an application which people can use to improve their health and wellbeing. My initial ideais the following:
An application that tracks the time since you last ate something, drank something and the last time you went outside. I also want this application to track the amount of time you spend outside that day.

Here are some ways I thought of to make use of AI regarding this idea:

---

1. Smart Reminders & Predictions
Instead of just reminding the user after a fixed time, an AI model could:  
 - Learn personal habits (e.g., you usually drink every 3 hours).  
 - Predict when the user is likely to forget to eat, drink, or go outside.  
 - Suggest reminders at the right time (not too early, not too late).  
 - Example: If the model sees you skipped meals yesterday at lunchtime, it might proactively nudge you earlier today.

---

2. Chatbot Coach
A conversational AI could:
- Let users log events by typing or speaking naturally (If a user types “I just had a sandwich” would be logged as eating).
- Answer questions like:
  - “How long since I last drank water?”
  - “How much time have I spent outside this week?”
- Offer encouragement or tips when it detects unhealthy habits.

---

3. Pattern Recognition & Insights
An AI system could analyze logs over time to provide personalized insights:
- “You usually go outside more on weekends than weekdays.”
- “You tend to drink less water on days you stay indoors.”
- “You average 25 minutes outside per day; increasing it to 45 minutes could improve your mood and energy.”  
- This can be rule-based at first, but AI can make correlations (e.g., weather vs. your outdoor time).  

---

After some work I have set up a small android application with a simple UI. <br> This UI consists of 3 simple buttons that can be pressed, one for eating, one for drinking and the last for going outside/back inside. ![Current UI](Images/image3.png) <br>

I have also designed some new pages I want to add to my application which consist of the Satistics and the EditData page. <br>

The EditData page should allow the user to add moments when they ate, drink or went outside at the end of the day incase they forgot:
![Idea of EditData page](Images/image.png)
<br>

The Statistics page should hold the general data of the user like the following:
- When the last time was since the user ate/drank/went outside
- How long the user has been outside today
- How often a user has eaten/drank something today
- etc.

![Idea of Statistics page](Images/image2.png)
<br>

Now I am working on getting the buttons to work. I need to find a way to save the time and date whenever a user presses one of the three buttons and send that data to a place where it can be stored and read by the AI.


I have made a small dataset with the following activities: 
<br> user_id,activity,timestamp<br>
Next I have made a simple converter file using python which converts the data from the dataset into features which the AI model can use to train. These are the features I am using to train the model: <br>
user_id,activity,timestamp,hour,day_of_week,is_weekend,minutes_since_midnight,time_since_last_drink,time_since_last_eat,time_since_last_outside,avg_drink_gap,avg_eat_gap,minutes_until_next <br>

I have also made a simple python file which is used for training the AI model.

