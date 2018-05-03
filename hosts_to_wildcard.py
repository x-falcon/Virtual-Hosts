#!python2
#coding:utf-8
'''
Copyright (C) 2017  xfalcon

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
'''
import socket
import re
p=re.compile(r"\s+")
def find_sub(s1,s2):
	if s1==s2:
		return s1
	if s1+"." in s2:
		return s1
	j=None
	while 1:
		i=s1.rfind(".",0,j)
		if i==-1:
			return ""
		str3=s1[0:i]
		if len(str3.split("."))<2:
			return ""
		if str3 in s2:
			return str3
		j=i

def query(s,dmap):
	if s in dmap:
		return dmap[s]
	else:
		j=0
		s="."+s
		while 1:
			i=s.find(".",j)
			if i==-1:
				return None
			temp_str=s[i:]
			if temp_str in dmap:
				return dmap[temp_str]
			j=i+1

def turn_to_wild(host_file,result_file):
	with open(host_file) as f:
		lines=f.readlines()
	dmap={}
	history_domain=[]
	for line in lines:
		try:
			ip,domain=p.split(line.strip())[0:2]
			socket.inet_aton(ip)
			if domain in history_domain:
				continue
			if ip in dmap:
				dmap[ip].add(domain)
			else:
				dmap[ip]=set([domain])
			history_domain.append(domain)
		except Exception as e:
			pass
	count_map=[]
	for ip,domains in dmap.iteritems():
		count_map.append((ip,len(domains)))
	count_map=sorted(count_map,key=lambda item:item[1],reverse=True)
	domain_ip={}
	for count in  count_map:
		ip=count[0]
		domains=dmap[ip]
		domains=sorted([x[::-1] for x in domains])
		first=""
		first=domains[0]
		for index,domain in enumerate(domains):
			temp_str=find_sub(first,domain)
			d_ip=query("."+temp_str[::-1],domain_ip)
			if d_ip and d_ip!=ip:
				domain_ip[first[::-1]]=ip
				domain_ip[domain[::-1]]=ip
				first=domain
				continue
			if not temp_str:
				if first==domains[index-1]:
					domain_ip[first[::-1]]=ip
				else:
					domain_ip["."+first[::-1]]=ip
				first=domain
				continue	
			first=temp_str
		if first==domains[-1]:
				domain_ip[first[::-1]]=ip
		else:
			domain_ip["."+first[::-1]]=ip
	with open(result_file,"w") as f:
		for domain,ip in domain_ip.iteritems():
			f.write(ip+"\t"+domain+"\n")


def vaild(source_file,dist_file):
	dist_map={}
	with open(dist_file) as f:
		for l in f.readlines():
			ip,domain=l.strip().split("\t")
			dist_map[domain]=ip
	count=0
	lines=0
	history_domain=[]
	with open(source_file) as f:
		for l in f.readlines():
			try:
				ip,domain=p.split(l.strip())[0:2]
				socket.inet_aton(ip)
				if domain in history_domain:
					continue
				history_domain.append(domain)
				lines+=1
				d_ip=query(domain,dist_map)
				if d_ip:
					if ip!=d_ip:
						print domain,ip,d_ip
					else:
						count+=1
				else:
					print domain+" not found"
			except Exception as e:
				pass
	print count,lines
if __name__ == '__main__':
	hosts_file="hosts.txt"
	turn_to_wild(hosts_file,hosts_file+".vhosts")
	vaild(hosts_file,hosts_file+".vhosts")