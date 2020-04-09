# Getting and storing data
## Create a chat repo
```kotlin
val chatRepo = ChatRepo.Builder(context).build()
```
## Get channels
```kotlin
val filter = and(eq("type", "messaging"), `in`("members", listOf(user.id)))
val channelsRepo:ChannelsRepo = chatRepo.channels
val channels:LiveData<List<Channel>> = channelsRepo.get(filter)
//or short
val channels = chatRepo.channels.get(filter)
```
## Create channel
```kotlin
channelsRepo.create(Channel(id = "hello"))
```
## Get messages
```kotlin
val messagesRepo:MessagesRepo = chatRepo.messages
val messages:LiveData<List<Message>> = messagesRepo.get(cid = "messages:hello")
//or short
val messages = chatRepo.messages.get(cid = "messages:hello")
```
## Create message
```kotlin
messagesRepo.create("messages:hello", Message(text = "hello"))
```
## Get users
```kotlin
val users:UsersRepo = chatRepo.users
val user:LiveData<User> = users.get(id = "Bob")
//or short
val user = chatRepo.users.get(id = "Bob")
```
## Create user
```kotlin
users.create(User(name = "Bob"))
```
# Controllers
Library contains set of useful controllers which might help you to work with chat data
## Unread messages of a channel
```kotlin
val channelController = chatRepo.controller.channel("messages:hello")
val unreadMessages:LiveData<Int> = channelController.unreadMessages()
```
## Typing events
```kotlin
val unreadMessages:ChatObservable = channelController.typingUsers()
```
## Sending messages
As well as with repositories you can send messages with channel controller
```kotlin
channelController.sendMessage(Message(test = "hello"))
```
## Members
```kotlin
val members:LiveData<List<User>> = channelController.getMembers()
```
