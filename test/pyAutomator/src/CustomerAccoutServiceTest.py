# importing the requests library
import requests

env = 'http://localhost:8100'

# /info request
print(requests.get(env + '/info').content)


print(requests.post(url = env + '/create-customer', params = {'name' : 'mohit rathod'}))